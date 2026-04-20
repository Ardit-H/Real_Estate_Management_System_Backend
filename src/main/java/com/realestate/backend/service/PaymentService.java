package com.realestate.backend.service;

import com.realestate.backend.dto.rental.PaymentDtos.*;
import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.entity.enums.PaymentType;
import com.realestate.backend.entity.rental.LeaseContract;
import com.realestate.backend.entity.rental.Payment;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.LeaseContractRepository;
import com.realestate.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository       paymentRepo;
    private final LeaseContractRepository contractRepo;

    private static final List<String> VALID_CURRENCIES = List.of("EUR","USD","GBP","CHF","ALL","MKD");
    private static final List<String> VALID_PAY_TYPES  = List.of("RENT","DEPOSIT","LATE_FEE","MAINTENANCE");

    // ── Queries ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByContract(Long contractId) {
        assertIsAdminOrAgent();
        if (contractId == null || contractId <= 0)
            throw new IllegalArgumentException("contractId invalid");
        return paymentRepo.findByContract_IdOrderByDueDateAsc(contractId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getByStatus(PaymentStatus status, Pageable pageable) {
        assertIsAdminOrAgent();
        if (status == null) throw new IllegalArgumentException("Status i detyrueshëm");
        return paymentRepo.findByStatusOrderByDueDateAsc(status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getOverdue() {
        assertIsAdminOrAgent();
        return paymentRepo.findOverduePayments(LocalDate.now())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(findPayment(id));
    }

    // ── Create ────────────────────────────────────────────────────
    @Transactional
    public PaymentResponse create(PaymentCreateRequest req) {
        assertIsAdminOrAgent();
        validateCreate(req);

        LeaseContract contract = contractRepo.findById(req.contractId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kontrata nuk u gjet: " + req.contractId()));

        Payment payment = Payment.builder()
                .contract(contract)
                .amount(req.amount())
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
                .paymentType(req.paymentType())
                .dueDate(req.dueDate())
                .paymentMethod(req.paymentMethod())
                .notes(req.notes())
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepo.save(payment);
        log.info("Payment created: id={}, contract={}, amount={}",
                saved.getId(), req.contractId(), req.amount());
        return toResponse(saved);
    }

    // ── Mark as paid ──────────────────────────────────────────────
    @Transactional
    public PaymentResponse markAsPaid(Long id, PaymentMarkPaidRequest req) {
        assertIsAdminOrAgent();
        Payment payment = findPayment(id);

        if (payment.getStatus() == PaymentStatus.PAID)
            throw new ConflictException("Pagesa është tashmë e paguar");
        if (payment.getStatus() == PaymentStatus.REFUNDED)
            throw new ConflictException("Pagesa e rimbursuar nuk mund të shënohet si e paguar");

        LocalDate paidDate = req.paidDate() != null ? req.paidDate() : LocalDate.now();
        if (paidDate.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("paidDate nuk mund të jetë në të ardhmen");

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidDate(paidDate);
        if (req.paymentMethod()  != null) payment.setPaymentMethod(req.paymentMethod());
        if (req.transactionRef() != null) payment.setTransactionRef(req.transactionRef());

        Payment saved = paymentRepo.save(payment);
        log.info("Payment id={} marked as PAID, paidDate={}", id, paidDate);
        return toResponse(saved);
    }

    // ── Update status ─────────────────────────────────────────────
    @Transactional
    public PaymentResponse updateStatus(Long id, PaymentStatusRequest req) {
        assertIsAdminOrAgent();
        if (req.status() == null) throw new IllegalArgumentException("Status i detyrueshëm");
        Payment payment = findPayment(id);
        payment.setStatus(req.status());
        return toResponse(paymentRepo.save(payment));
    }

    // ── Mark overdue (background job) ────────────────────────────
    @Transactional
    public int markOverduePayments() {
        List<Payment> overdue = paymentRepo.findOverduePayments(LocalDate.now());
        overdue.forEach(p -> p.setStatus(PaymentStatus.OVERDUE));
        paymentRepo.saveAll(overdue);
        log.info("{} payments marked as OVERDUE", overdue.size());
        return overdue.size();
    }

    // ── Summary ───────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PaymentSummaryResponse getSummaryByContract(Long contractId) {
        assertIsAdminOrAgent();
        List<PaymentResponse> payments = paymentRepo
                .findByContract_IdOrderByDueDateAsc(contractId)
                .stream().map(this::toResponse).toList();

        BigDecimal totalPaid = paymentRepo.totalPaidByContract(contractId);
        BigDecimal totalPending = payments.stream()
                .filter(p -> p.status() == PaymentStatus.PENDING
                        || p.status() == PaymentStatus.OVERDUE)
                .map(PaymentResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long overdueCount = payments.stream()
                .filter(p -> p.status() == PaymentStatus.OVERDUE).count();

        return new PaymentSummaryResponse(
                payments.size(), totalPaid, totalPending, (int) overdueCount, payments);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        assertIsAdminOrAgent();
        return paymentRepo.totalRevenue();
    }

    private void validateCreate(PaymentCreateRequest req) {
        if (req.contractId() == null || req.contractId() <= 0)
            throw new IllegalArgumentException("contractId i detyrueshëm");

        if (req.amount() == null)
            throw new IllegalArgumentException("Amount i detyrueshëm");
        if (req.amount().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Amount >= 0");
        if (req.amount().compareTo(new BigDecimal("999999999")) > 0)
            throw new IllegalArgumentException("Amount shumë i madh");

        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase()))
            throw new IllegalArgumentException("Currency e pavlefshme: " + req.currency());

        if (req.dueDate() == null)
            throw new IllegalArgumentException("dueDate i detyrueshëm");

        if (req.notes() != null && req.notes().length() > 1000)
            throw new IllegalArgumentException("Notes max 1000 karaktere");

        if (req.paymentMethod() != null && req.paymentMethod().length() > 50)
            throw new IllegalArgumentException("paymentMethod max 50 karaktere");
    }

    private Payment findPayment(Long id) {
        return paymentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagesa nuk u gjet: " + id));
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT"))
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
    }

    private PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getContract() != null ? p.getContract().getId() : null,
                p.getAmount(), p.getCurrency(),
                p.getPaymentType(), p.getDueDate(), p.getPaidDate(),
                p.getPaymentMethod(), p.getTransactionRef(),
                p.getStatus(), p.getNotes(), p.getCreatedAt()
        );
    }
}