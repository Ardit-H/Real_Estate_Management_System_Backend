package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.profile.AgentProfile;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.AgentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentProfileService {

    private final AgentProfileRepository agentProfileRepo;

    // ── Vlera të lejuara ──────────────────────────────────────────────────────
    private static final int    MAX_PHONE_LENGTH         = 30;
    private static final int    MAX_LICENSE_LENGTH       = 100;
    private static final int    MAX_SPECIALIZATION_LEN   = 100;
    private static final int    MAX_EXPERIENCE_YEARS     = 60;
    private static final BigDecimal MAX_RATING           = BigDecimal.valueOf(5);
    private static final BigDecimal MIN_RATING           = BigDecimal.ZERO;

    // ── Merr profilin tim ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AgentProfileResponse getMyProfile() {
        Long userId = TenantContext.getUserId();
        return agentProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profili i agjentit nuk u gjet. Krijo profilin me PUT /api/users/agents/me"));
    }

    // ── Merr profilin sipas userId ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public AgentProfileResponse getByUserId(Long userId) {
        return agentProfileRepo.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profili i agjentit nuk u gjet për user: " + userId));
    }

    // ── Lista e të gjithë agjentëve (sipas vlerësimit) ────────────────────────
    @Transactional(readOnly = true)
    public List<AgentProfileResponse> getAllAgents() {
        return agentProfileRepo.findAllByOrderByRatingDesc()
                .stream().map(this::toResponse).toList();
    }

    // ── Krijo ose ndrysho profilin tim ───────────────────────────────────────
    @Transactional
    public AgentProfileResponse upsertMyProfile(AgentProfileRequest req) {
        assertIsAgent();
        Long userId = TenantContext.getUserId();

        // ── Validime ─────────────────────────────────────────────────────────
        validateAgentProfileRequest(req);

        AgentProfile profile = agentProfileRepo.findByUserId(userId)
                .orElseGet(() -> AgentProfile.builder().userId(userId).build());

        applyAgentProfileFields(profile, req);

        AgentProfile saved = agentProfileRepo.save(profile);
        log.info("AgentProfile u ruajt për userId={}", userId);
        return toResponse(saved);
    }

    // ── ADMIN: ndrysho profilin e çdo agjenti ────────────────────────────────
    @Transactional
    public AgentProfileResponse updateProfile(Long userId, AgentProfileRequest req) {
        if (!TenantContext.hasRole("ADMIN")) {
            throw new ForbiddenException("Vetëm ADMIN mund të ndryshojë profil tjetër");
        }

        // ── Validime ─────────────────────────────────────────────────────────
        validateAgentProfileRequest(req);

        AgentProfile profile = agentProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profili nuk u gjet për user: " + userId));

        applyAgentProfileFields(profile, req);

        AgentProfile saved = agentProfileRepo.save(profile);
        log.info("AgentProfile i userId={} u ndryshua nga ADMIN", userId);
        return toResponse(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validateAgentProfileRequest(AgentProfileRequest req) {
        if (req.phone() != null) {
            String cleaned = req.phone().replaceAll("[\\s\\-()]", "");
            if (!cleaned.matches("^\\+?[0-9]{6,15}$")) {
                throw new BadRequestException(
                        "Numri i telefonit është i pavlefshëm. Format i pranueshëm: +3834XXXXXXX ose 04XXXXXXX");
            }
            if (req.phone().length() > MAX_PHONE_LENGTH) {
                throw new BadRequestException(
                        "Numri i telefonit nuk mund të jetë më i gjatë se " + MAX_PHONE_LENGTH + " karaktere");
            }
        }
        if (req.license() != null && req.license().length() > MAX_LICENSE_LENGTH) {
            throw new BadRequestException(
                    "Licença nuk mund të jetë më e gjatë se " + MAX_LICENSE_LENGTH + " karaktere");
        }
        if (req.experienceYears() != null) {
            if (req.experienceYears() < 0) {
                throw new BadRequestException("Vitet e përvojës nuk mund të jenë negative");
            }
            if (req.experienceYears() > MAX_EXPERIENCE_YEARS) {
                throw new BadRequestException(
                        "Vitet e përvojës nuk mund të jenë më shumë se " + MAX_EXPERIENCE_YEARS);
            }
        }
        if (req.specialization() != null && req.specialization().length() > MAX_SPECIALIZATION_LEN) {
            throw new BadRequestException(
                    "Specializimi nuk mund të jetë më i gjatë se " + MAX_SPECIALIZATION_LEN + " karaktere");
        }
        if (req.photoUrl() != null && !req.photoUrl().isBlank()) {
            if (!req.photoUrl().matches("^https?://.*")) {
                throw new BadRequestException("URL e fotos duhet të fillojë me http:// ose https://");
            }
            if (req.photoUrl().length() > 500) {
                throw new BadRequestException("URL e fotos nuk mund të jetë më e gjatë se 500 karaktere");
            }
        }
    }

    private void applyAgentProfileFields(AgentProfile profile, AgentProfileRequest req) {
        if (req.phone()           != null) profile.setPhone(req.phone().trim());
        if (req.license()         != null) profile.setLicense(req.license().trim());
        if (req.bio()             != null) profile.setBio(req.bio().trim());
        if (req.experienceYears() != null) profile.setExperienceYears(req.experienceYears());
        if (req.specialization()  != null) profile.setSpecialization(req.specialization().trim());
        if (req.photoUrl()        != null) profile.setPhotoUrl(req.photoUrl().trim());
    }

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