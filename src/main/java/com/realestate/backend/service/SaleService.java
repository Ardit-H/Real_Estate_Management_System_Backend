package com.realestate.backend.service;

import com.realestate.backend.dto.sale.SaleDtos.*;
import com.realestate.backend.entity.User;
import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.sale.SaleContract;
import com.realestate.backend.entity.sale.SaleListing;
import com.realestate.backend.entity.sale.SalePayment;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleListingRepository  listingRepo;
    private final SaleContractRepository contractRepo;
    private final SalePaymentRepository  paymentRepo;
    private final PropertyRepository     propertyRepo;
    private final UserRepository         userRepo;

    // Vlerat e lejuara (reflektojnë CHECK constraints në DB)
    private static final Set<String> VALID_CURRENCIES     = Set.of("EUR", "USD", "ALL", "GBP", "CHF");
    private static final Set<String> VALID_CONTRACT_STATUS = Set.of("PENDING", "COMPLETED", "CANCELLED");
    private static final Set<String> VALID_PAYMENT_TYPES  = Set.of("DEPOSIT", "INSTALLMENT", "FULL", "COMMISSION", "AGENT_COMMISSION", "CLIENT_BONUS");
    private static final Set<String> VALID_PAYMENT_STATUS = Set.of("PENDING", "PAID", "FAILED", "REFUNDED");
    private static final Set<String> VALID_PAYMENT_METHODS =
            Set.of("BANK_TRANSFER", "CASH", "CARD", "CHECK", "ONLINE");

    // ================ SALE LISTINGS ==============================

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

        // Validime
        if (req.price() == null || req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Çmimi duhet të jetë 0 ose më i madh");
        }
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase())) {
            throw new BadRequestException("Monedha e pavlefshme: " + req.currency()
                    + ". Vlerat e lejuara: " + VALID_CURRENCIES);
        }

        Property property = propertyRepo.findByIdAndDeletedAtIsNull(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Prona nuk u gjet: " + req.propertyId()));

        SaleListing listing = SaleListing.builder()
                .property(property)
                .agentId(TenantContext.getUserId())
                .price(req.price())
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
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

        // Validime
        if (req.price() != null && req.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Çmimi nuk mund të jetë negativ");
        }
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase())) {
            throw new BadRequestException("Monedha e pavlefshme: " + req.currency());
        }

        if (req.price()       != null) listing.setPrice(req.price());
        if (req.currency()    != null) listing.setCurrency(req.currency().toUpperCase());
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

    // ======================= SALE CONTRACTS ==========================

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

        // Validime
        if (req.salePrice() == null || req.salePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Çmimi i shitjes duhet të jetë 0 ose më i madh");
        }
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase())) {
            throw new BadRequestException("Monedha e pavlefshme: " + req.currency());
        }
        if (req.contractDate() != null && req.handoverDate() != null
                && req.handoverDate().isBefore(req.contractDate())) {
            throw new BadRequestException("Data e dorëzimit nuk mund të jetë para datës së kontratës");
        }

        // Kontratë aktive ekziston tashmë?
        contractRepo.findByProperty_IdAndStatus(req.propertyId(), "PENDING")
                .ifPresent(c -> {
                    throw new ConflictException("Prona ka tashmë kontratë PENDING: " + c.getId());
                });

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
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
                .contractDate(req.contractDate())
                .handoverDate(req.handoverDate())
                .contractFileUrl(req.contractFileUrl())
                .status("PENDING")
                .build();

        SaleContract saved = contractRepo.save(contract);
        log.info("SaleContract u krijua: id={}, property={}, buyer={}",
                saved.getId(), req.propertyId(), req.buyerId());
        return toContractResponse(saved);
    }

    @Transactional
    public SaleContractResponse updateContract(Long id, SaleContractUpdateRequest req) {
        assertIsAdminOrAgent();
        SaleContract contract = findContract(id);

        // Validime
        if ("COMPLETED".equals(contract.getStatus()) || "CANCELLED".equals(contract.getStatus())) {
            throw new ConflictException("Kontratat e mbyllura nuk mund të ndryshohen");
        }
        if (req.salePrice() != null && req.salePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Çmimi i shitjes nuk mund të jetë negativ");
        }
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase())) {
            throw new BadRequestException("Monedha e pavlefshme: " + req.currency());
        }

        LocalDate newContractDate = req.contractDate() != null ? req.contractDate() : contract.getContractDate();
        LocalDate newHandoverDate = req.handoverDate() != null ? req.handoverDate() : contract.getHandoverDate();
        if (newContractDate != null && newHandoverDate != null && newHandoverDate.isBefore(newContractDate)) {
            throw new BadRequestException("Data e dorëzimit nuk mund të jetë para datës së kontratës");
        }

        if (req.salePrice()       != null) contract.setSalePrice(req.salePrice());
        if (req.currency()        != null) contract.setCurrency(req.currency().toUpperCase());
        if (req.contractDate()    != null) contract.setContractDate(req.contractDate());
        if (req.handoverDate()    != null) contract.setHandoverDate(req.handoverDate());
        if (req.contractFileUrl() != null) contract.setContractFileUrl(req.contractFileUrl());

        return toContractResponse(contractRepo.save(contract));
    }

    @Transactional
    public SaleContractResponse updateContractStatus(Long id, SaleContractStatusRequest req) {
        assertIsAdminOrAgent();
        SaleContract contract = findContract(id);

        // Validime
        if (!VALID_CONTRACT_STATUS.contains(req.status())) {
            throw new BadRequestException("Statusi i pavlefshëm: " + req.status()
                    + ". Vlerat e lejuara: " + VALID_CONTRACT_STATUS);
        }
        // Tranzicionet e lejuara: PENDING → COMPLETED | CANCELLED
        // Nuk mund të kthehesh nga COMPLETED ose CANCELLED
        if ("COMPLETED".equals(contract.getStatus()) || "CANCELLED".equals(contract.getStatus())) {
            throw new ConflictException("Kontrata me status '"
                    + contract.getStatus() + "' nuk mund të ndryshohet");
        }
        if ("PENDING".equals(req.status())) {
            throw new BadRequestException("Kontrata nuk mund të kthehet në PENDING");
        }

        contractRepo.updateStatus(id, req.status());
        if ("COMPLETED".equals(req.status())) {
            SaleContract fresh = findContract(id);
            createCommissionPayments(fresh);
        }
        log.info("SaleContract id={} statusi u ndryshua në {}", id, req.status());
        return toContractResponse(findContract(id));
    }

    // ======================== SALE PAYMENTS ==========================

    @Transactional(readOnly = true)
    public List<SalePaymentResponse> getPaymentsByContract(Long contractId) {
        assertIsAdminOrAgent();
        findContract(contractId); // kontrollo ekzistencën
        return paymentRepo.findByContract_IdOrderByCreatedAtAsc(contractId)
                .stream().map(this::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalePaymentSummaryResponse getPaymentSummary(Long contractId) {
        assertIsAdminOrAgent();
        findContract(contractId);
        List<SalePaymentResponse> payments = paymentRepo
                .findByContract_IdOrderByCreatedAtAsc(contractId)
                .stream().map(this::toPaymentResponse).toList();
        BigDecimal totalPaid = paymentRepo.totalPaidByContract(contractId);
        return new SalePaymentSummaryResponse(payments.size(), totalPaid, payments);
    }

    @Transactional
    public SalePaymentResponse createPayment(SalePaymentCreateRequest req) {
        assertIsAdminOrAgent();

        // Validime
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Shuma duhet të jetë 0 ose më e madhe");
        }
        if (req.currency() != null && !VALID_CURRENCIES.contains(req.currency().toUpperCase())) {
            throw new BadRequestException("Monedha e pavlefshme: " + req.currency());
        }
        if (req.paymentType() != null && !VALID_PAYMENT_TYPES.contains(req.paymentType().toUpperCase())) {
            throw new BadRequestException("Tipi i pagesës i pavlefshëm: " + req.paymentType()
                    + ". Vlerat e lejuara: " + VALID_PAYMENT_TYPES);
        }
        if (req.paymentMethod() != null && !VALID_PAYMENT_METHODS.contains(req.paymentMethod().toUpperCase())) {
            throw new BadRequestException("Metoda e pagesës e pavlefshme: " + req.paymentMethod()
                    + ". Vlerat e lejuara: " + VALID_PAYMENT_METHODS);
        }

        SaleContract contract = findContract(req.contractId());

        // Nuk mund të shtohen pagesa në kontratë të anuluar/kompletuar
        if ("CANCELLED".equals(contract.getStatus())) {
            throw new InvalidStateException("Nuk mund të krijohet pagesë për kontratë të CANCELLED");
        }
        User recipient = null;
        if (req.recipientId() != null) {
            recipient = userRepo.findById(req.recipientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User nuk u gjet: " + req.recipientId()));
        }
        SalePayment payment = SalePayment.builder()
                .contract(contract)
                .amount(req.amount())
                .currency(req.currency() != null ? req.currency().toUpperCase() : "EUR")
                .paymentType(req.paymentType() != null ? req.paymentType().toUpperCase() : "FULL")
                .paymentMethod(req.paymentMethod() != null ? req.paymentMethod().toUpperCase() : null)
                .recipient(recipient)
                .status("PENDING")
                .build();

        SalePayment saved = paymentRepo.save(payment);
        log.info("SalePayment u krijua: id={}, contract={}, amount={},recipient={}",
                saved.getId(), req.contractId(), req.amount(),
                recipient != null ? recipient.getId() : "COMPANY");
        return toPaymentResponse(saved);
    }

    private void createCommissionPayments(SaleContract contract) {
        BigDecimal commissionRate  = new BigDecimal("0.03");
        BigDecimal commissionTotal = contract.getSalePrice().multiply(commissionRate);

        // 1. Kompania — recipient NULL
        paymentRepo.save(SalePayment.builder()
                .contract(contract)
                .amount(commissionTotal.multiply(new BigDecimal("0.50")))
                .currency(contract.getCurrency())
                .paymentType("COMMISSION")
                .recipient(null)
                .status("PENDING")
                .build());

        // 2. Agjenti — 40%
        userRepo.findById(contract.getAgentId()).ifPresent(agent -> {
            paymentRepo.save(SalePayment.builder()
                    .contract(contract)
                    .amount(commissionTotal.multiply(new BigDecimal("0.40")))
                    .currency(contract.getCurrency())
                    .paymentType("AGENT_COMMISSION")
                    .recipient(agent)
                    .status("PENDING")
                    .build());
        });

        // 3. Blerësi — 10%
        if (contract.getBuyerId() != null) {
            userRepo.findById(contract.getBuyerId()).ifPresent(buyer -> {
                paymentRepo.save(SalePayment.builder()
                        .contract(contract)
                        .amount(commissionTotal.multiply(new BigDecimal("0.10")))
                        .currency(contract.getCurrency())
                        .paymentType("CLIENT_BONUS")
                        .recipient(buyer)
                        .status("PENDING")
                        .build());
            });
        }

        log.info("Commission payments u krijuan për contract={}, total={}",
                contract.getId(), commissionTotal);
    }

    @Transactional
    public SalePaymentResponse markPaymentAsPaid(Long id, SalePaymentMarkPaidRequest req) {
        assertIsAdminOrAgent();

        SalePayment payment = paymentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagesa nuk u gjet: " + id));

        // Validime
        if ("PAID".equals(payment.getStatus())) {
            throw new ConflictException("Pagesa #" + id + " është tashmë e paguar");
        }
        if ("FAILED".equals(payment.getStatus()) || "REFUNDED".equals(payment.getStatus())) {
            throw new InvalidStateException("Pagesa me status '" + payment.getStatus()
                    + "' nuk mund të shënohet si PAID");
        }
        if (req.paidDate() != null && req.paidDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Data e pagesës nuk mund të jetë në të ardhmen");
        }
        if (req.paymentMethod() != null
                && !VALID_PAYMENT_METHODS.contains(req.paymentMethod().toUpperCase())) {
            throw new BadRequestException("Metoda e pagesës e pavlefshme: " + req.paymentMethod());
        }

        payment.setStatus("PAID");
        payment.setPaidDate(req.paidDate() != null ? req.paidDate() : LocalDate.now());
        if (req.paymentMethod()  != null) payment.setPaymentMethod(req.paymentMethod().toUpperCase());
        if (req.transactionRef() != null) payment.setTransactionRef(req.transactionRef());

        return toPaymentResponse(paymentRepo.save(payment));
    }

    @Transactional(readOnly = true)
    public Page<SaleListingResponse> getMyListings(Pageable pageable) {
        assertIsAdminOrAgent();
        return listingRepo.findByAgentIdAndDeletedAtIsNull(TenantContext.getUserId(), pageable)
                .map(this::toListingResponse);
    }

    // Helpers

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

    // Mappers

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
        Long   recipientId   = null;
        String recipientName = null;
        String recipientType = "COMPANY"; // default kur NULL

        if (p.getRecipient() != null) {
            recipientId   = p.getRecipient().getId();
            recipientName = p.getRecipient().getFullName();
            recipientType = p.getRecipient().getRole().name(); // AGENT / CLIENT
        }

        return new SalePaymentResponse(
                p.getId(),
                p.getContract() != null ? p.getContract().getId() : null,
                p.getAmount(), p.getCurrency(), p.getPaymentType(),
                p.getPaidDate(), p.getPaymentMethod(), p.getTransactionRef(),
                recipientId, recipientName, recipientType,  // ← TRE fusha të reja
                p.getStatus(), p.getCreatedAt()
        );
    }
}
