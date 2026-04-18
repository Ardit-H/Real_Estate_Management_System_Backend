package com.realestate.backend.service;

import com.realestate.backend.dto.sale.SaleDtos.*;
import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.sale.SaleContract;
import com.realestate.backend.entity.sale.SaleListing;
import com.realestate.backend.entity.sale.SalePayment;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.PropertyRepository;
import com.realestate.backend.repository.SaleContractRepository;
import com.realestate.backend.repository.SaleListingRepository;
import com.realestate.backend.repository.SalePaymentRepository;
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
public class SaleService {

    private final SaleListingRepository   listingRepo;
    private final SaleContractRepository  contractRepo;
    private final SalePaymentRepository   paymentRepo;
    private final PropertyRepository      propertyRepo;

    // ══════════════════ SALE LISTINGS ══════════════════════════

    @Transactional(readOnly = true)
    public Page<SaleListingResponse> getAllListings(Pageable pageable) {
        return listingRepo.findAllByDeletedAtIsNull(pageable).map(this::toListingResponse);
    }

    @Transactional(readOnly = true)
    public SaleListingResponse getListingById(Long id) {
        return toListingResponse(findActiveListing(id));
    }

    @Transactional(readOnly = true)
    public Page<SaleListingResponse> getListingsByStatus(SaleStatus status, Pageable pageable) {
        return listingRepo.findByStatusAndDeletedAtIsNull(status, pageable).map(this::toListingResponse);
    }

    @Transactional(readOnly = true)
    public List<SaleListingResponse> getListingsByProperty(Long propertyId) {
        return listingRepo.findByProperty_IdAndDeletedAtIsNull(propertyId)
                .stream().map(this::toListingResponse).toList();
    }

    @Transactional
    public SaleListingResponse createListing(SaleListingCreateRequest req) {
        assertIsAdminOrAgent();

        Property property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + req.propertyId()));

        SaleListing listing = SaleListing.builder()
                .property(property)
                .agentId(TenantContext.getUserId())
                .price(req.price())
                .currency(req.currency() != null ? req.currency() : "EUR")
                .negotiable(req.negotiable() != null ? req.negotiable() : true)
                .description(req.description())
                .highlights(req.highlights())
                .status(SaleStatus.ACTIVE)
                .build();

        SaleListing saved = listingRepo.save(listing);
        log.info("SaleListing u krijua: id={}, property={}", saved.getId(), req.propertyId());
        return toListingResponse(saved);
    }

    @Transactional
    public SaleListingResponse updateListing(Long id, SaleListingUpdateRequest req) {
        assertIsAdminOrAgent();
        SaleListing listing = findActiveListing(id);
        assertCanModifyListing(listing);

        if (req.price()       != null) listing.setPrice(req.price());
        if (req.currency()    != null) listing.setCurrency(req.currency());
        if (req.negotiable()  != null) listing.setNegotiable(req.negotiable());
        if (req.description() != null) listing.setDescription(req.description());
        if (req.highlights()  != null) listing.setHighlights(req.highlights());
        if (req.status()      != null) listing.setStatus(req.status());

        return toListingResponse(listingRepo.save(listing));
    }

    @Transactional
    public void deleteListing(Long id) {
        findActiveListing(id);
        assertIsAdminOrAgent();
        listingRepo.softDelete(id);
        log.info("SaleListing id={} u fshi (soft delete)", id);
    }

    // ══════════════════ SALE CONTRACTS ══════════════════════════

    @Transactional(readOnly = true)
    public Page<SaleContractResponse> getAllContracts(Pageable pageable) {
        assertIsAdminOrAgent();
        return contractRepo.findByStatusOrderByCreatedAtDesc("PENDING", pageable).map(this::toContractResponse);
    }

    @Transactional(readOnly = true)
    public SaleContractResponse getContractById(Long id) {
        return toContractResponse(findContract(id));
    }

    @Transactional(readOnly = true)
    public Page<SaleContractResponse> getContractsByBuyer(Long buyerId, Pageable pageable) {
        return contractRepo.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable).map(this::toContractResponse);
    }

    @Transactional(readOnly = true)
    public Page<SaleContractResponse> getContractsByAgent(Long agentId, Pageable pageable) {
        assertIsAdminOrAgent();
        return contractRepo.findByAgentIdOrderByCreatedAtDesc(agentId, pageable).map(this::toContractResponse);
    }

    @Transactional(readOnly = true)
    public List<SaleContractResponse> getContractsByProperty(Long propertyId) {
        assertIsAdminOrAgent();
        return contractRepo.findByProperty_IdOrderByCreatedAtDesc(propertyId)
                .stream().map(this::toContractResponse).toList();
    }

    @Transactional
    public SaleContractResponse createContract(SaleContractCreateRequest req) {
        assertIsAdminOrAgent();

        contractRepo.findByProperty_IdAndStatus(req.propertyId(), "PENDING")
                .ifPresent(c -> { throw new ConflictException("Prona ka tashmë kontratë PENDING: " + c.getId()); });

        Property property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + req.propertyId()));

        SaleListing listing = null;
        if (req.listingId() != null) {
            listing = findActiveListing(req.listingId());
        }

        SaleContract contract = SaleContract.builder()
                .property(property)
                .listing(listing)
                .buyerId(req.buyerId())
                .agentId(TenantContext.getUserId())
                .salePrice(req.salePrice())
                .currency(req.currency() != null ? req.currency() : "EUR")
                .contractDate(req.contractDate())
                .handoverDate(req.handoverDate())
                .contractFileUrl(req.contractFileUrl())
                .status("PENDING")
                .build();

        SaleContract saved = contractRepo.save(contract);
        log.info("SaleContract u krijua: id={}, property={}, buyer={}", saved.getId(), req.propertyId(), req.buyerId());
        return toContractResponse(saved);
    }

    @Transactional
    public SaleContractResponse updateContract(Long id, SaleContractUpdateRequest req) {
        assertIsAdminOrAgent();
        SaleContract contract = findContract(id);

        if ("COMPLETED".equals(contract.getStatus()) || "CANCELLED".equals(contract.getStatus())) {
            throw new ConflictException("Kontratat e mbyllura nuk mund të ndryshohen");
        }

        if (req.salePrice()       != null) contract.setSalePrice(req.salePrice());
        if (req.currency()        != null) contract.setCurrency(req.currency());
        if (req.contractDate()    != null) contract.setContractDate(req.contractDate());
        if (req.handoverDate()    != null) contract.setHandoverDate(req.handoverDate());
        if (req.contractFileUrl() != null) contract.setContractFileUrl(req.contractFileUrl());

        return toContractResponse(contractRepo.save(contract));
    }

    @Transactional
    public SaleContractResponse updateContractStatus(Long id, SaleContractStatusRequest req) {
        assertIsAdminOrAgent();
        findContract(id);
        contractRepo.updateStatus(id, req.status());
        log.info("SaleContract id={} statusi u ndryshua në {}", id, req.status());
        return toContractResponse(findContract(id));
    }

    // ══════════════════ SALE PAYMENTS ═══════════════════════════

    @Transactional(readOnly = true)
    public List<SalePaymentResponse> getPaymentsByContract(Long contractId) {
        assertIsAdminOrAgent();
        return paymentRepo.findByContract_IdOrderByCreatedAtAsc(contractId)
                .stream().map(this::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalePaymentSummaryResponse getPaymentSummary(Long contractId) {
        assertIsAdminOrAgent();
        List<SalePaymentResponse> payments = paymentRepo
                .findByContract_IdOrderByCreatedAtAsc(contractId)
                .stream().map(this::toPaymentResponse).toList();
        BigDecimal totalPaid = paymentRepo.totalPaidByContract(contractId);
        return new SalePaymentSummaryResponse(payments.size(), totalPaid, payments);
    }

    @Transactional
    public SalePaymentResponse createPayment(SalePaymentCreateRequest req) {
        assertIsAdminOrAgent();
        SaleContract contract = findContract(req.contractId());

        SalePayment payment = SalePayment.builder()
                .contract(contract)
                .amount(req.amount())
                .currency(req.currency() != null ? req.currency() : "EUR")
                .paymentType(req.paymentType() != null ? req.paymentType() : "FULL")
                .paymentMethod(req.paymentMethod())
                .status("PENDING")
                .build();

        SalePayment saved = paymentRepo.save(payment);
        log.info("SalePayment u krijua: id={}, contract={}, amount={}", saved.getId(), req.contractId(), req.amount());
        return toPaymentResponse(saved);
    }

    @Transactional
    public SalePaymentResponse markPaymentAsPaid(Long id, SalePaymentMarkPaidRequest req) {
        assertIsAdminOrAgent();
        SalePayment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagesa nuk u gjet: " + id));

        if ("PAID".equals(payment.getStatus())) {
            throw new ConflictException("Pagesa është tashmë e paguar");
        }

        payment.setStatus("PAID");
        payment.setPaidDate(req.paidDate() != null ? req.paidDate() : LocalDate.now());
        if (req.paymentMethod()  != null) payment.setPaymentMethod(req.paymentMethod());
        if (req.transactionRef() != null) payment.setTransactionRef(req.transactionRef());

        return toPaymentResponse(paymentRepo.save(payment));
    }

    // ── Helpers ───────────────────────────────────────────────

    private SaleListing findActiveListing(Long id) {
        return listingRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("SaleListing nuk u gjet: " + id));
    }

    private SaleContract findContract(Long id) {
        return contractRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SaleContract nuk u gjet: " + id));
    }

    private void assertIsAdminOrAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm ADMIN ose AGENT mund të kryejë këtë veprim");
        }
    }

    private void assertCanModifyListing(SaleListing listing) {
        if (TenantContext.hasRole("ADMIN")) return;
        if (!listing.getAgentId().equals(TenantContext.getUserId())) {
            throw new ForbiddenException("Nuk keni leje për këtë listing");
        }
    }

    // ── Mappers ───────────────────────────────────────────────

    private SaleListingResponse toListingResponse(SaleListing l) {
        return new SaleListingResponse(
                l.getId(),
                l.getProperty() != null ? l.getProperty().getId() : null,
                l.getAgentId(), l.getPrice(), l.getCurrency(),
                l.getNegotiable(), l.getDescription(), l.getHighlights(),
                l.getStatus(), l.getCreatedAt(), l.getUpdatedAt()
        );
    }

    private SaleContractResponse toContractResponse(SaleContract c) {
        return new SaleContractResponse(
                c.getId(),
                c.getProperty() != null ? c.getProperty().getId() : null,
                c.getListing()  != null ? c.getListing().getId()  : null,
                c.getBuyerId(), c.getAgentId(),
                c.getSalePrice(), c.getCurrency(),
                c.getContractDate(), c.getHandoverDate(),
                c.getContractFileUrl(), c.getStatus(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    private SalePaymentResponse toPaymentResponse(SalePayment p) {
        return new SalePaymentResponse(
                p.getId(),
                p.getContract() != null ? p.getContract().getId() : null,
                p.getAmount(), p.getCurrency(), p.getPaymentType(),
                p.getPaidDate(), p.getPaymentMethod(), p.getTransactionRef(),
                p.getStatus(), p.getCreatedAt()
        );
    }
}
