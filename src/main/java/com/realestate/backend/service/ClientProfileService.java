package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.profile.ClientProfile;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.ClientProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final ClientProfileRepository clientProfileRepo;

    // ── Merr profilin tim ─────────────────────────────────────
    @Transactional(readOnly = true)
    public ClientProfileResponse getMyProfile() {
        Long userId = TenantContext.getUserId();
        return clientProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Profili i klientit nuk u gjet"));
    }

    // ── ADMIN/AGENT: merr profilin e klientit ─────────────────
    @Transactional(readOnly = true)
    public ClientProfileResponse getByUserId(Long userId) {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Nuk keni leje");
        }
        return clientProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Profili nuk u gjet: " + userId));
    }

    // ── Krijo ose ndrysho profilin tim ────────────────────────
    @Transactional
    public ClientProfileResponse upsertMyProfile(ClientProfileRequest req) {
        Long userId = TenantContext.getUserId();

        ClientProfile profile = clientProfileRepo.findByUserId(userId)
                .orElseGet(() -> ClientProfile.builder().userId(userId).build());

        if (req.phone()            != null) profile.setPhone(req.phone());
        if (req.preferredContact() != null) profile.setPreferredContact(req.preferredContact());
        if (req.budgetMin()        != null) profile.setBudgetMin(req.budgetMin());
        if (req.budgetMax()        != null) profile.setBudgetMax(req.budgetMax());
        if (req.preferredType()    != null) profile.setPreferredType(req.preferredType());
        if (req.preferredCity()    != null) profile.setPreferredCity(req.preferredCity());
        if (req.photoUrl()         != null) profile.setPhotoUrl(req.photoUrl());

        ClientProfile saved = clientProfileRepo.save(profile);
        log.info("ClientProfile u ruajt për userId={}", userId);
        return toResponse(saved);
    }

    // ── Mapper ────────────────────────────────────────────────

    private ClientProfileResponse toResponse(ClientProfile p) {
        return new ClientProfileResponse(
                p.getId(), p.getUserId(), p.getPhone(),
                p.getPreferredContact(), p.getBudgetMin(), p.getBudgetMax(),
                p.getPreferredType(), p.getPreferredCity(), p.getPhotoUrl()
        );
    }
}
