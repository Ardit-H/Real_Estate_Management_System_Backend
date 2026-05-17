package com.realestate.backend.service;

import com.realestate.backend.dto.sale.SaleDtos.*;
import com.realestate.backend.entity.User;
import com.realestate.backend.entity.enums.ListingType;
import com.realestate.backend.entity.enums.NotificationType;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.SaleStatus;
import com.realestate.backend.entity.property.Property;
import com.realestate.backend.entity.sale.SaleContract;
import com.realestate.backend.entity.sale.SaleListing;
import com.realestate.backend.entity.sale.SalePayment;
import com.realestate.backend.entity.lead.PropertyLeadRequest;
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
    private final LeadRequestRepository leadRequestRepo;
    private final NotificationService notificationService;

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

        if (property.getListingType() == ListingType.RENT) {
            throw new ConflictException(
                    "Kjo pronë është e konfiguruar vetëm për qira — nuk mund të krijohet sale listing."
            );
        }
        if (property.getStatus() == PropertyStatus.SOLD) {
            throw new ConflictException(
                    "Kjo pronë është shitur — nuk mund të krijohet sale listing i ri."
            );
        }
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

        // Kontro nese prona eshte tashmë e shitur
        if (property.getStatus() == PropertyStatus.SOLD) {
            throw new ConflictException(
                    "Kjo pronë është tashmë e shitur dhe nuk mund të blihet sërish."
            );
        }
// Kontro nese ka kontrate COMPLETED per kete prone
        contractRepo.findByProperty_IdAndStatus(req.propertyId(), "COMPLETED")
                .ifPresent(c -> {
                    throw new ConflictException(
                            "Prona ka tashmë një kontratë shitjeje të kompletuar (id=" + c.getId() + ")."
                    );
                });
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
        propertyRepo.updateStatus(req.propertyId(), PropertyStatus.PENDING);
        notificationService.sendNotification(
                req.buyerId(),
                "Sale Contract Created",
                "A sale contract has been created for property #" + req.propertyId() + ". Please review.",
                NotificationType.INFO,
                "sale_contract", saved.getId(),
                "/client/mysalecontracts"
        );
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
            propertyRepo.updateStatus(fresh.getProperty().getId(), PropertyStatus.SOLD);
            // Sheno edhe listing-at si SOLD
            listingRepo.findByProperty_IdAndDeletedAtIsNull(fresh.getProperty().getId())
                    .forEach(l -> listingRepo.updateStatus(l.getId(), SaleStatus.SOLD));
            createCommissionPayments(fresh);
            notificationService.sendNotification(
                    fresh.getBuyerId(),
                    "Sale Completed ✓",
                    "Congratulations! Your purchase of property #" + fresh.getProperty().getId() + " is complete.",
                    NotificationType.SUCCESS,
                    "sale_contract", id,
                    "/client/mysalecontracts"
            );
            notificationService.sendNotification(
                    fresh.getAgentId(),
                    "Sale Contract Completed",
                    "Sale contract #" + id + " has been completed. Commission payments created.",
                    NotificationType.SUCCESS,
                    "sale_contract", id,
                    "/agent/sales"
            );
        }
        if ("CANCELLED".equals(req.status())) {
            // Kontro nese ka kontrate tjeter PENDING per kete prone
            boolean hasOther = contractRepo.findByProperty_IdOrderByCreatedAtDesc(
                    contract.getProperty().getId()
            ).stream().anyMatch(c -> !c.getId().equals(id) && "PENDING".equals(c.getStatus()));

            PropertyStatus revertTo = hasOther ? PropertyStatus.PENDING : PropertyStatus.AVAILABLE;
            propertyRepo.updateStatus(contract.getProperty().getId(), revertTo);
        }
        log.info("SaleContract id={} statusi u ndryshua në {}", id, req.status());
        return toContractResponse(findContract(id));
    }

    // ======================== SALE PAYMENTS ==========================

    @Transactional(readOnly = true)
    public List<SalePaymentResponse> getPaymentsByContract(Long contractId) {
        SaleContract contract = findContract(contractId);

        // CLIENT mund të shohë vetëm pagesat e kontratave të tij
        if (TenantContext.hasRole("CLIENT")) {
            if (!contract.getBuyerId().equals(TenantContext.getUserId())) {
                throw new ForbiddenException("Nuk keni akses në këtë kontratë");
            }
        } else {
            assertIsAdminOrAgent();
        }

        return paymentRepo.findByContract_IdOrderByCreatedAtAsc(contractId)
                .stream().map(this::toPaymentResponse).toList();
    }

    @Transactional(readOnly = true)
    public SalePaymentSummaryResponse getPaymentSummary(Long contractId) {
        SaleContract contract = findContract(contractId);

        // CLIENT mund të shohë vetëm summary e kontratave të tij
        if (TenantContext.hasRole("CLIENT")) {
            if (!contract.getBuyerId().equals(TenantContext.getUserId())) {
                throw new ForbiddenException("Nuk keni akses në këtë kontratë");
            }
        } else {
            assertIsAdminOrAgent();
        }

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

        // Nuk mund të shtohen pagesa manuale në kontratë COMPLETED
        if ("CANCELLED".equals(contract.getStatus())
                || "COMPLETED".equals(contract.getStatus())) {
            throw new InvalidStateException(
                    "Nuk mund të krijohet pagesë manuale për kontratë "
                            + contract.getStatus());
        }

        Set<String> AUTO_ONLY_TYPES = Set.of("FULL", "COMMISSION",
                "AGENT_COMMISSION", "CLIENT_BONUS");
        if (req.paymentType() != null
                && AUTO_ONLY_TYPES.contains(req.paymentType().toUpperCase())) {
            throw new BadRequestException(
                    "Tipi '" + req.paymentType() + "' krijohet vetëm automatikisht. " +
                            "Pagesat manuale lejojnë vetëm: DEPOSIT, INSTALLMENT");
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
        BigDecimal salePrice       = contract.getSalePrice();
        BigDecimal commissionRate  = new BigDecimal("0.03");
        BigDecimal commissionTotal = salePrice.multiply(commissionRate);
        BigDecimal ownerAmount     = salePrice.multiply(new BigDecimal("0.97"));
        String     currency        = contract.getCurrency();

        // ← E RE: llogarit shumën e depozitave dhe kësteve të paguara tashmë
        BigDecimal alreadyPaid = paymentRepo
                .findByContract_IdOrderByCreatedAtAsc(contract.getId())
                .stream()
                .filter(p -> p.getStatus().equals("PAID")
                        && (p.getPaymentType().equals("DEPOSIT")
                        || p.getPaymentType().equals("INSTALLMENT")))
                .map(SalePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ownerAmount i mbetur = 97% - çfarë është paguar tashmë
        BigDecimal remainingOwnerAmount = ownerAmount.subtract(alreadyPaid);

        // Sigurohu që nuk shkojmë negativ
        if (remainingOwnerAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingOwnerAmount = BigDecimal.ZERO;
        }

        log.info("Contract={} alreadyPaid={} remainingOwner={}",
                contract.getId(), alreadyPaid, remainingOwnerAmount);

        Long ownerClientId = leadRequestRepo
                .findByPropertyIdOrdered(contract.getProperty().getId())
                .stream()
                .filter(l -> l.getStatus().name().equals("DONE"))
                .findFirst()
                .map(PropertyLeadRequest::getClientId)
                .orElse(null);

        boolean isClientOwnedProperty = ownerClientId != null;

        if (isClientOwnedProperty) {
            // ══════════════════════════════════════════════════
            // SKENARI 1 — Pronë e klientit
            // ══════════════════════════════════════════════════
            User owner = userRepo.findById(ownerClientId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Pronari nuk u gjet: " + ownerClientId));

            // FULL — vetëm shuma e mbetur pas depozitave
            // Nëse alreadyPaid = 0 → FULL = 97% e plotë
            // Nëse alreadyPaid = 10,000 → FULL = 97,000 - 10,000 = 87,000
            if (remainingOwnerAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentRepo.save(SalePayment.builder()
                        .contract(contract)
                        .amount(remainingOwnerAmount)
                        .currency(currency)
                        .paymentType("FULL")
                        .recipient(owner)
                        .status("PENDING")
                        .build());
            }

            // COMMISSION — 50% → kompania (nuk ndikohet nga depozita)
            paymentRepo.save(SalePayment.builder()
                    .contract(contract)
                    .amount(commissionTotal.multiply(new BigDecimal("0.50")))
                    .currency(currency)
                    .paymentType("COMMISSION")
                    .recipient(null)
                    .status("PENDING")
                    .build());

            // AGENT_COMMISSION — 40% → agjenti
            userRepo.findById(contract.getAgentId()).ifPresent(agent ->
                    paymentRepo.save(SalePayment.builder()
                            .contract(contract)
                            .amount(commissionTotal.multiply(new BigDecimal("0.40")))
                            .currency(currency)
                            .paymentType("AGENT_COMMISSION")
                            .recipient(agent)
                            .status("PENDING")
                            .build())
            );

            // CLIENT_BONUS — 10% → pronari
            paymentRepo.save(SalePayment.builder()
                    .contract(contract)
                    .amount(commissionTotal.multiply(new BigDecimal("0.10")))
                    .currency(currency)
                    .paymentType("CLIENT_BONUS")
                    .recipient(owner)
                    .status("PENDING")
                    .build());

            log.info("Skenari 1: FULL={} COMMISSION={} AGENT={} BONUS={} (alreadyPaid={})",
                    remainingOwnerAmount,
                    commissionTotal.multiply(new BigDecimal("0.50")),
                    commissionTotal.multiply(new BigDecimal("0.40")),
                    commissionTotal.multiply(new BigDecimal("0.10")),
                    alreadyPaid);

        } else {
            // ══════════════════════════════════════════════════
            // SKENARI 2 — Pronë e kompanisë
            // ══════════════════════════════════════════════════

            // FULL — vetëm shuma e mbetur
            if (remainingOwnerAmount.compareTo(BigDecimal.ZERO) > 0) {
                paymentRepo.save(SalePayment.builder()
                        .contract(contract)
                        .amount(remainingOwnerAmount)
                        .currency(currency)
                        .paymentType("FULL")
                        .recipient(null)
                        .status("PENDING")
                        .build());
            }

            // COMMISSION — 60% → kompania
            paymentRepo.save(SalePayment.builder()
                    .contract(contract)
                    .amount(commissionTotal.multiply(new BigDecimal("0.60")))
                    .currency(currency)
                    .paymentType("COMMISSION")
                    .recipient(null)
                    .status("PENDING")
                    .build());

            // AGENT_COMMISSION — 40% → agjenti
            userRepo.findById(contract.getAgentId()).ifPresent(agent ->
                    paymentRepo.save(SalePayment.builder()
                            .contract(contract)
                            .amount(commissionTotal.multiply(new BigDecimal("0.40")))
                            .currency(currency)
                            .paymentType("AGENT_COMMISSION")
                            .recipient(agent)
                            .status("PENDING")
                            .build())
            );

            log.info("Skenari 2: FULL={} COMMISSION={} AGENT={} (alreadyPaid={})",
                    remainingOwnerAmount,
                    commissionTotal.multiply(new BigDecimal("0.60")),
                    commissionTotal.multiply(new BigDecimal("0.40")),
                    alreadyPaid);
        }
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
