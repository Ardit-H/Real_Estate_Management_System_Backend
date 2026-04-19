package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.profile.AgentProfile;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.AgentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileService {

    private final AgentProfileRepository agentProfileRepo;

    // ── Merr profilin tim ─────────────────────────────────────
    @Transactional(readOnly = true)
    public AgentProfileResponse getMyProfile() {
        Long userId = TenantContext.getUserId();
        return agentProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Profili i agjentit nuk u gjet"));
    }

    // ── Merr profilin sipas userId ────────────────────────────
    @Transactional(readOnly = true)
    public AgentProfileResponse getByUserId(Long userId) {
        return agentProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Profili i agjentit nuk u gjet për user: " + userId));
    }

    // ── Lista e të gjithë agjentëve ───────────────────────────
    @Transactional(readOnly = true)
    public List<AgentProfileResponse> getAllAgents() {
        return agentProfileRepo.findAllByOrderByRatingDesc()
                .stream().map(this::toResponse).toList();
    }

    // ── Krijo ose ndrysho profilin tim ────────────────────────
    @Transactional
    public AgentProfileResponse upsertMyProfile(AgentProfileRequest req) {
        assertIsAgent();
        Long userId = TenantContext.getUserId();

        AgentProfile profile = agentProfileRepo.findByUserId(userId)
                .orElseGet(() -> AgentProfile.builder().userId(userId).build());

        if (req.phone()           != null) profile.setPhone(req.phone());
        if (req.license()         != null) profile.setLicense(req.license());
        if (req.bio()             != null) profile.setBio(req.bio());
        if (req.experienceYears() != null) profile.setExperienceYears(req.experienceYears());
        if (req.specialization()  != null) profile.setSpecialization(req.specialization());
        if (req.photoUrl()        != null) profile.setPhotoUrl(req.photoUrl());

        AgentProfile saved = agentProfileRepo.save(profile);
        log.info("AgentProfile u ruajt për userId={}", userId);
        return toResponse(saved);
    }

    // ── ADMIN: ndrysho profilin e çdo agjenti ─────────────────
    @Transactional
    public AgentProfileResponse updateProfile(Long userId, AgentProfileRequest req) {
        if (!TenantContext.hasRole("ADMIN")) {
            throw new ForbiddenException("Vetëm ADMIN mund të ndryshojë profil tjetër");
        }

        AgentProfile profile = agentProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profili nuk u gjet: " + userId));

        if (req.phone()           != null) profile.setPhone(req.phone());
        if (req.license()         != null) profile.setLicense(req.license());
        if (req.bio()             != null) profile.setBio(req.bio());
        if (req.experienceYears() != null) profile.setExperienceYears(req.experienceYears());
        if (req.specialization()  != null) profile.setSpecialization(req.specialization());
        if (req.photoUrl()        != null) profile.setPhotoUrl(req.photoUrl());

        return toResponse(agentProfileRepo.save(profile));
    }

    // ── Helpers ───────────────────────────────────────────────

    private void assertIsAgent() {
        if (!TenantContext.hasRole("ADMIN", "AGENT")) {
            throw new ForbiddenException("Vetëm AGENT mund të menaxhojë profilin e agjentit");
        }
    }

    private AgentProfileResponse toResponse(AgentProfile p) {
        return new AgentProfileResponse(
                p.getId(), p.getUserId(), p.getPhone(), p.getLicense(),
                p.getBio(), p.getExperienceYears(), p.getSpecialization(),
                p.getPhotoUrl(), p.getRating(), p.getTotalReviews()
        );
    }
}
