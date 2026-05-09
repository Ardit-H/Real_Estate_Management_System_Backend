package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.profile.AgentProfile;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.AgentProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgentProfileService — Unit Tests")
class AgentProfileServiceTest {

    @Mock AgentProfileRepository agentProfileRepo;

    @InjectMocks AgentProfileService service;

    private MockedStatic<TenantContext> tenantCtx;
    private static final Long AGENT_ID = 5L;
    private static final Long ADMIN_ID = 1L;

    @BeforeEach
    void openTenant() {
        tenantCtx = mockStatic(TenantContext.class);
    }

    @AfterEach
    void closeTenant() {
        tenantCtx.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void asAgent(Long id) {
        tenantCtx.when(TenantContext::getUserId).thenReturn(id);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(true);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(false);
    }

    private void asAdmin(Long id) {
        tenantCtx.when(TenantContext::getUserId).thenReturn(id);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(true);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(true);
    }

    private void asClient(Long id) {
        tenantCtx.when(TenantContext::getUserId).thenReturn(id);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(false);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(false);
    }

    private AgentProfile buildProfile(Long id, Long userId) {
        AgentProfile p = AgentProfile.builder()
                .userId(userId)
                .phone("+38344123456")
                .license("LIC-001")
                .bio("Bio e agjentit")
                .experienceYears(5)
                .specialization("Residential")
                .photoUrl("https://example.com/photo.jpg")
                .build();
        try {
            var f = AgentProfile.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        return p;
    }

    private AgentProfileRequest validRequest() {
        return new AgentProfileRequest(
                "+38344123456", "LIC-001", "Bio ime",
                5, "Residential", "https://example.com/photo.jpg");
    }

    // ── getMyProfile() ───────────────────────────────────────────

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("sukses — kthen profilin e agjentit aktual")
        void success() {
            asAgent(AGENT_ID);
            AgentProfile profile = buildProfile(1L, AGENT_ID);
            when(agentProfileRepo.findByUserId(AGENT_ID)).thenReturn(Optional.of(profile));

            AgentProfileResponse resp = service.getMyProfile();
            assertThat(resp.userId()).isEqualTo(AGENT_ID);
            assertThat(resp.license()).isEqualTo("LIC-001");
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur profili nuk ekziston")
        void throws_whenNotFound() {
            asAgent(AGENT_ID);
            when(agentProfileRepo.findByUserId(AGENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMyProfile())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("PUT");
        }
    }

    // ── getByUserId() ────────────────────────────────────────────

    @Nested
    @DisplayName("getByUserId()")
    class GetByUserId {

        @Test
        @DisplayName("sukses — kthen profilin sipas userId")
        void success() {
            AgentProfile profile = buildProfile(1L, AGENT_ID);
            when(agentProfileRepo.findByUserId(AGENT_ID)).thenReturn(Optional.of(profile));

            AgentProfileResponse resp = service.getByUserId(AGENT_ID);
            assertThat(resp.userId()).isEqualTo(AGENT_ID);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur userId nuk gjendet")
        void throws_whenNotFound() {
            when(agentProfileRepo.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByUserId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── getAllAgents() ────────────────────────────────────────────

    @Nested
    @DisplayName("getAllAgents()")
    class GetAllAgents {

        @Test
        @DisplayName("kthen listën e renditur sipas rating")
        void returnsSortedList() {
            AgentProfile p1 = buildProfile(1L, 1L);
            AgentProfile p2 = buildProfile(2L, 2L);
            when(agentProfileRepo.findAllByOrderByRatingDesc()).thenReturn(List.of(p1, p2));

            List<AgentProfileResponse> result = service.getAllAgents();
            assertThat(result).hasSize(2);
        }
    }

    // ── upsertMyProfile() ────────────────────────────────────────

    @Nested
    @DisplayName("upsertMyProfile()")
    class UpsertMyProfile {

        @Test
        @DisplayName("sukses — krijon profil të ri kur nuk ekziston")
        void success_creates() {
            asAgent(AGENT_ID);
            when(agentProfileRepo.findByUserId(AGENT_ID)).thenReturn(Optional.empty());
            when(agentProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AgentProfileResponse resp = service.upsertMyProfile(validRequest());
            assertThat(resp.userId()).isEqualTo(AGENT_ID);
            assertThat(resp.phone()).isEqualTo("+38344123456");
        }

        @Test
        @DisplayName("sukses — update profil ekzistues")
        void success_updates() {
            asAgent(AGENT_ID);
            AgentProfile existing = buildProfile(1L, AGENT_ID);
            when(agentProfileRepo.findByUserId(AGENT_ID)).thenReturn(Optional.of(existing));
            when(agentProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AgentProfileRequest req = new AgentProfileRequest(
                    "+38344999999", null, null, null, null, null);
            AgentProfileResponse resp = service.upsertMyProfile(req);
            assertThat(resp.phone()).isEqualTo("+38344999999");
        }

        @Test
        @DisplayName("CLIENT — hedh ForbiddenException")
        void client_throws() {
            asClient(99L);
            assertThatThrownBy(() -> service.upsertMyProfile(validRequest()))
                    .isInstanceOf(ForbiddenException.class);
        }

        // Validime

        @Test
        @DisplayName("hedh BadRequestException kur telefoni ka format të gabuar")
        void throws_whenPhoneInvalid() {
            asAgent(AGENT_ID);
            AgentProfileRequest req = new AgentProfileRequest(
                    "abc", null, null, null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("telefon");
        }

        @Test
        @DisplayName("hedh BadRequestException kur experienceYears është negative")
        void throws_whenExperienceNegative() {
            asAgent(AGENT_ID);
            AgentProfileRequest req = new AgentProfileRequest(
                    null, null, null, -1, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("negative");
        }

        @Test
        @DisplayName("hedh BadRequestException kur experienceYears > 60")
        void throws_whenExperienceTooHigh() {
            asAgent(AGENT_ID);
            AgentProfileRequest req = new AgentProfileRequest(
                    null, null, null, 61, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("60");
        }

        @Test
        @DisplayName("hedh BadRequestException kur license tejkalon 100 karaktere")
        void throws_whenLicenseTooLong() {
            asAgent(AGENT_ID);
            AgentProfileRequest req = new AgentProfileRequest(
                    null, "L".repeat(101), null, null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("hedh BadRequestException kur photoUrl nuk fillon me http")
        void throws_whenPhotoUrlInvalid() {
            asAgent(AGENT_ID);
            AgentProfileRequest req = new AgentProfileRequest(
                    null, null, null, null, null, "ftp://bad-url.com");

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("http");
        }

        @Test
        @DisplayName("hedh BadRequestException kur specialization tejkalon 100 karaktere")
        void throws_whenSpecializationTooLong() {
            asAgent(AGENT_ID);
            AgentProfileRequest req = new AgentProfileRequest(
                    null, null, null, null, "S".repeat(101), null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ── updateProfile() (ADMIN) ──────────────────────────────────

    @Nested
    @DisplayName("updateProfile() — ADMIN")
    class UpdateProfile {

        @Test
        @DisplayName("ADMIN — ndrysho profilin e çdo agjenti")
        void admin_updatesAny() {
            asAdmin(ADMIN_ID);
            AgentProfile existing = buildProfile(1L, AGENT_ID);
            when(agentProfileRepo.findByUserId(AGENT_ID)).thenReturn(Optional.of(existing));
            when(agentProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AgentProfileResponse resp = service.updateProfile(AGENT_ID, validRequest());
            assertThat(resp.userId()).isEqualTo(AGENT_ID);
        }

        @Test
        @DisplayName("jo-ADMIN — hedh ForbiddenException")
        void nonAdmin_throws() {
            asAgent(AGENT_ID);
            // hasRole("ADMIN") = false
            tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> service.updateProfile(AGENT_ID, validRequest()))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur profili nuk ekziston")
        void throws_whenNotFound() {
            asAdmin(ADMIN_ID);
            when(agentProfileRepo.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateProfile(99L, validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}