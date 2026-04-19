package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.profile.ClientProfile;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.ClientProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final ClientProfileRepository clientProfileRepo;

    // Vlera të lejuara — reflektojnë CHECK constraint në DB
    // preferred_contact VARCHAR(20) CHECK (preferred_contact IN ('EMAIL','PHONE','WHATSAPP'))
    private static final Set<String> VALID_CONTACT_METHODS = Set.of("EMAIL", "PHONE", "WHATSAPP");

    // Merr profilin tim
    @Transactional(readOnly = true)
    public ClientProfileResponse getMyProfile() {
        Long userId = TenantContext.getUserId();
        return clientProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profili i klientit nuk u gjet. Krijo profilin me PUT /api/users/clients/me"));
    }

    // ADMIN/AGENT: merr profilin e klientit
    @Transactional(readOnly = true)
    public ClientProfileResponse getByUserId(Long userId) {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Nuk keni leje të shikoni profilin e klientit");
        }
        return clientProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profili i klientit nuk u gjet për user: " + userId));
    }

    // Krijo ose ndrysho profilin tim
    @Transactional
    public ClientProfileResponse upsertMyProfile(ClientProfileRequest req) {
        Long userId = TenantContext.getUserId();

        // Validime
        validateClientProfileRequest(req);

        ClientProfile profile = clientProfileRepo.findByUserId(userId)
                .orElseGet(() -> ClientProfile.builder().userId(userId).build());

        applyClientProfileFields(profile, req);

        ClientProfile saved = clientProfileRepo.save(profile);
        log.info("ClientProfile u ruajt për userId={}", userId);
        return toResponse(saved);
    }

    // Helpers

    private void validateClientProfileRequest(ClientProfileRequest req) {

        // preferred_contact CHECK constraint: EMAIL | PHONE | WHATSAPP
        if (req.preferredContact() != null
                && !VALID_CONTACT_METHODS.contains(req.preferredContact().toUpperCase())) {
            throw new BadRequestException(
                    "Metoda e kontaktit e pavlefshme: '" + req.preferredContact()
                            + "'. Vlerat e lejuara: " + VALID_CONTACT_METHODS);
        }

        // budget_min dhe budget_max — nuk mund të jenë negative
        if (req.budgetMin() != null && req.budgetMin().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Buxheti minimal nuk mund të jetë negativ");
        }
        if (req.budgetMax() != null && req.budgetMax().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Buxheti maksimal nuk mund të jetë negativ");
        }

        // budget_min nuk mund të jetë më i madh se budget_max
        if (req.budgetMin() != null && req.budgetMax() != null
                && req.budgetMin().compareTo(req.budgetMax()) > 0) {
            throw new BadRequestException(
                    "Buxheti minimal (" + req.budgetMin()
                            + ") nuk mund të jetë më i madh se buxheti maksimal (" + req.budgetMax() + ")");
        }

        // phone — format bazik
        if (req.phone() != null && !req.phone().isBlank()) {
            String cleaned = req.phone().replaceAll("[\\s\\-()]", "");
            if (!cleaned.matches("^\\+?[0-9]{6,15}$")) {
                throw new BadRequestException(
                        "Numri i telefonit është i pavlefshëm. Format i pranueshëm: +3834XXXXXXX");
            }
            if (req.phone().length() > 30) {
                throw new BadRequestException("Numri i telefonit nuk mund të jetë më i gjatë se 30 karaktere");
            }
        }

        // preferred_type — VARCHAR(50)
        if (req.preferredType() != null && req.preferredType().length() > 50) {
            throw new BadRequestException("Tipi i preferuar nuk mund të jetë më i gjatë se 50 karaktere");
        }

        // preferred_city — VARCHAR(100)
        if (req.preferredCity() != null && req.preferredCity().length() > 100) {
            throw new BadRequestException("Qyteti i preferuar nuk mund të jetë më i gjatë se 100 karaktere");
        }

        // photo_url — VARCHAR(500) + format HTTP
        if (req.photoUrl() != null && !req.photoUrl().isBlank()) {
            if (!req.photoUrl().matches("^https?://.*")) {
                throw new BadRequestException("URL e fotos duhet të fillojë me http:// ose https://");
            }
            if (req.photoUrl().length() > 500) {
                throw new BadRequestException("URL e fotos nuk mund të jetë më e gjatë se 500 karaktere");
            }
        }
    }

    private void applyClientProfileFields(ClientProfile profile, ClientProfileRequest req) {
        if (req.phone()            != null) profile.setPhone(req.phone().trim());
        if (req.preferredContact() != null) profile.setPreferredContact(req.preferredContact().toUpperCase());
        if (req.budgetMin()        != null) profile.setBudgetMin(req.budgetMin());
        if (req.budgetMax()        != null) profile.setBudgetMax(req.budgetMax());
        if (req.preferredType()    != null) profile.setPreferredType(req.preferredType().trim());
        if (req.preferredCity()    != null) profile.setPreferredCity(req.preferredCity().trim());
        if (req.photoUrl()         != null) profile.setPhotoUrl(req.photoUrl().trim());
    }

    // Mapper

    private ClientProfileResponse toResponse(ClientProfile p) {
        return new ClientProfileResponse(
                p.getId(), p.getUserId(), p.getPhone(),
                p.getPreferredContact(), p.getBudgetMin(), p.getBudgetMax(),
                p.getPreferredType(), p.getPreferredCity(), p.getPhotoUrl()
        );
    }
}