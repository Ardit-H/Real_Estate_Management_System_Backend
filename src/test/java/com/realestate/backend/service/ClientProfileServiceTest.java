package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.profile.ClientProfile;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.ClientProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientProfileService — Unit Tests")
class ClientProfileServiceTest {

    @Mock ClientProfileRepository clientProfileRepo;

    @InjectMocks ClientProfileService service;

    private MockedStatic<TenantContext> tenantCtx;
    private static final Long CLIENT_ID = 20L;
    private static final Long AGENT_ID  = 30L;

    @BeforeEach
    void openTenant() {
        tenantCtx = mockStatic(TenantContext.class);
    }

    @AfterEach
    void closeTenant() {
        tenantCtx.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void asClient(Long id) {
        tenantCtx.when(TenantContext::getUserId).thenReturn(id);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(false);
    }

    private void asAdminOrAgent(Long id) {
        tenantCtx.when(TenantContext::getUserId).thenReturn(id);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(true);
    }

    private ClientProfile buildProfile(Long id, Long userId) {
        ClientProfile p = ClientProfile.builder()
                .userId(userId)
                .phone("+38344123456")
                .preferredContact("EMAIL")
                .budgetMin(new BigDecimal("50000"))
                .budgetMax(new BigDecimal("200000"))
                .preferredType("APARTMENT")
                .preferredCity("Tirana")
                .photoUrl("https://example.com/photo.jpg")
                .build();
        try {
            var f = ClientProfile.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (Exception ignored) {}
        return p;
    }

    private ClientProfileRequest validRequest() {
        return new ClientProfileRequest(
                "+38344123456", "EMAIL",
                new BigDecimal("50000"), new BigDecimal("200000"),
                "APARTMENT", "Tirana",
                "https://example.com/photo.jpg");
    }

    // ── getMyProfile() ───────────────────────────────────────────

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("sukses — kthen profilin e klientit aktual")
        void success() {
            asClient(CLIENT_ID);
            ClientProfile profile = buildProfile(1L, CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.of(profile));

            ClientProfileResponse resp = service.getMyProfile();
            assertThat(resp.userId()).isEqualTo(CLIENT_ID);
            assertThat(resp.preferredContact()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur profili mungon")
        void throws_whenNotFound() {
            asClient(CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.empty());

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
        @DisplayName("ADMIN/AGENT — kthen profilin e klientit")
        void adminOrAgent_success() {
            asAdminOrAgent(AGENT_ID);
            ClientProfile profile = buildProfile(1L, CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.of(profile));

            ClientProfileResponse resp = service.getByUserId(CLIENT_ID);
            assertThat(resp.userId()).isEqualTo(CLIENT_ID);
        }

        @Test
        @DisplayName("CLIENT — hedh ForbiddenException")
        void client_throws() {
            asClient(CLIENT_ID);
            assertThatThrownBy(() -> service.getByUserId(99L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur profili nuk ekziston")
        void throws_whenNotFound() {
            asAdminOrAgent(AGENT_ID);
            when(clientProfileRepo.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByUserId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── upsertMyProfile() ────────────────────────────────────────

    @Nested
    @DisplayName("upsertMyProfile()")
    class UpsertMyProfile {

        @Test
        @DisplayName("sukses — krijon profil të ri")
        void success_creates() {
            asClient(CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.empty());
            when(clientProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClientProfileResponse resp = service.upsertMyProfile(validRequest());
            assertThat(resp.userId()).isEqualTo(CLIENT_ID);
            assertThat(resp.preferredContact()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("sukses — update profil ekzistues")
        void success_updates() {
            asClient(CLIENT_ID);
            ClientProfile existing = buildProfile(1L, CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.of(existing));
            when(clientProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClientProfileRequest req = new ClientProfileRequest(
                    null, "PHONE", null, null, null, null, null);
            ClientProfileResponse resp = service.upsertMyProfile(req);
            assertThat(resp.preferredContact()).isEqualTo("PHONE");
        }

        @Test
        @DisplayName("preferredContact ruhet uppercase")
        void preferredContact_savedUpperCase() {
            asClient(CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.empty());
            when(clientProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClientProfileRequest req = new ClientProfileRequest(
                    null, "whatsapp", null, null, null, null, null);
            ClientProfileResponse resp = service.upsertMyProfile(req);
            assertThat(resp.preferredContact()).isEqualTo("WHATSAPP");
        }

        // Validime

        @Test
        @DisplayName("hedh BadRequestException kur preferredContact është i pavlefshëm")
        void throws_whenContactMethodInvalid() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, "TELEGRAM", null, null, null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("TELEGRAM");
        }

        @Test
        @DisplayName("hedh BadRequestException kur budgetMin është negativ")
        void throws_whenBudgetMinNegative() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, null, new BigDecimal("-1"), null, null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("minimal");
        }

        @Test
        @DisplayName("hedh BadRequestException kur budgetMax është negativ")
        void throws_whenBudgetMaxNegative() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, null, null, new BigDecimal("-100"), null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("maksimal");
        }

        @Test
        @DisplayName("hedh BadRequestException kur budgetMin > budgetMax")
        void throws_whenMinGreaterThanMax() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, null,
                    new BigDecimal("300000"), new BigDecimal("100000"),
                    null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("minimal");
        }

        @Test
        @DisplayName("hedh BadRequestException kur telefoni ka format të gabuar")
        void throws_whenPhoneInvalid() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    "not-a-phone", null, null, null, null, null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("telefon");
        }

        @Test
        @DisplayName("hedh BadRequestException kur photoUrl nuk fillon me http")
        void throws_whenPhotoUrlInvalid() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, null, null, null, null, null, "ftp://bad.com");

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("http");
        }

        @Test
        @DisplayName("hedh BadRequestException kur preferredCity tejkalon 100 karaktere")
        void throws_whenCityTooLong() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, null, null, null, null, "C".repeat(101), null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("hedh BadRequestException kur preferredType tejkalon 50 karaktere")
        void throws_whenTypeTooLong() {
            asClient(CLIENT_ID);
            ClientProfileRequest req = new ClientProfileRequest(
                    null, null, null, null, "T".repeat(51), null, null);

            assertThatThrownBy(() -> service.upsertMyProfile(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("50");
        }

        @Test
        @DisplayName("budgetMin == budgetMax lejohet")
        void equalBudgets_allowed() {
            asClient(CLIENT_ID);
            when(clientProfileRepo.findByUserId(CLIENT_ID)).thenReturn(Optional.empty());
            when(clientProfileRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ClientProfileRequest req = new ClientProfileRequest(
                    null, null,
                    new BigDecimal("100000"), new BigDecimal("100000"),
                    null, null, null);

            assertThatNoException().isThrownBy(() -> service.upsertMyProfile(req));
        }
    }
}