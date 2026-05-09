package com.realestate.backend.service;

import com.realestate.backend.dto.auth.*;
import com.realestate.backend.entity.RefreshToken;
import com.realestate.backend.entity.User;
import com.realestate.backend.entity.enums.Role;
import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.UnauthorizedException;
import com.realestate.backend.repository.*;
import com.realestate.backend.security.jwt.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Unit Tests")
class AuthServiceTest {

    @Mock UserRepository             userRepository;
    @Mock TenantRepository           tenantRepository;
    @Mock SchemaRegistryRepository   schemaRegistryRepository;
    @Mock RefreshTokenRepository     refreshTokenRepository;
    @Mock PasswordEncoder            passwordEncoder;
    @Mock JwtUtil                    jwtUtil;
    @Mock SchemaProvisioningService  provisioningService;
    @Mock RoleRepository             roleRepository;
    @Mock UserRoleRepository         userRoleRepository;

    @InjectMocks AuthService service;

    private static final String IP      = "127.0.0.1";
    private static final String UA      = "Mozilla/5.0";
    private static final String SCHEMA  = "tenant_abc";
    private static final String ACCESS  = "access-token";
    private static final String REFRESH = "refresh-token";

    // ── Helpers ──────────────────────────────────────────────────

    private TenantCompany activeTenant(Long id, String slug) {
        TenantCompany t = new TenantCompany();
        t.setId(id); t.setSlug(slug); t.setName("TestCo");
        t.setIsActive(true); t.setPlan("FREE");
        return t;
    }

    private User activeUser(Long id, TenantCompany tenant) {
        User u = new User();
        u.setId(id);
        u.setEmail("user@test.com");
        u.setPassword("encoded-pass");
        u.setFirstName("Test"); u.setLastName("User");
        u.setRole(Role.CLIENT);
        u.setTenant(tenant);
        u.setIsActive(true);
        return u;
    }

    private void stubTokenGeneration() {
        when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any())).thenReturn(ACCESS);
        when(jwtUtil.generateRefreshToken(any(), any())).thenReturn(REFRESH);
    }

    // ── register() ───────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("sukses — krijon user dhe tenant të ri")
        void success_newTenant() {
            RegisterRequest req = new RegisterRequest(
                    "new@test.com", "Pass1234", "Ardit", "H",
                    "CLIENT", "new-tenant", null);

            when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
            when(tenantRepository.findBySlug("new-tenant")).thenReturn(Optional.empty());

            TenantCompany savedTenant = activeTenant(1L, "new-tenant");
            when(tenantRepository.save(any())).thenReturn(savedTenant);
            when(passwordEncoder.encode("Pass1234")).thenReturn("encoded");

            User savedUser = activeUser(10L, savedTenant);
            savedUser.setEmail("new@test.com");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(roleRepository.findByName(any())).thenReturn(Optional.empty());
            when(provisioningService.provisionIfNeeded(savedTenant)).thenReturn(SCHEMA);
            stubTokenGeneration();

            AuthResponse resp = service.register(req, IP, UA);

            assertThat(resp.accessToken()).isEqualTo(ACCESS);
            assertThat(resp.refreshToken()).isEqualTo(REFRESH);
            assertThat(resp.email()).isEqualTo("new@test.com");
            verify(tenantRepository).save(any());
            verify(userRepository).save(any());
            verify(refreshTokenRepository).save(any());
        }

        @Test
        @DisplayName("sukses — tenant ekzistues rifoloshet")
        void success_existingTenant() {
            RegisterRequest req = new RegisterRequest(
                    "agent@test.com", "Pass1234", "Ana", "B",
                    "AGENT", "existing-slug", "ExistingCo");

            when(userRepository.existsByEmail("agent@test.com")).thenReturn(false);
            TenantCompany existing = activeTenant(2L, "existing-slug");
            when(tenantRepository.findBySlug("existing-slug")).thenReturn(Optional.of(existing));
            when(passwordEncoder.encode(any())).thenReturn("encoded");

            User savedUser = activeUser(11L, existing);
            savedUser.setEmail("agent@test.com");
            when(userRepository.save(any())).thenReturn(savedUser);
            when(roleRepository.findByName(any())).thenReturn(Optional.empty());
            when(provisioningService.provisionIfNeeded(existing)).thenReturn(SCHEMA);
            stubTokenGeneration();

            AuthResponse resp = service.register(req, IP, UA);

            assertThat(resp.tenantId()).isEqualTo(2L);
            verify(tenantRepository, never()).save(any());
        }

        @Test
        @DisplayName("hedh ConflictException kur email ekziston")
        void throws_whenEmailExists() {
            RegisterRequest req = new RegisterRequest(
                    "dup@test.com", "Pass1234", "A", "B", null, "slug", null);
            when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register(req, IP, UA))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur tenant është i çaktivizuar")
        void throws_whenTenantInactive() {
            RegisterRequest req = new RegisterRequest(
                    "x@test.com", "Pass1234", "A", "B", null, "inactive-slug", null);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            TenantCompany inactive = activeTenant(3L, "inactive-slug");
            inactive.setIsActive(false);
            when(tenantRepository.findBySlug("inactive-slug")).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> service.register(req, IP, UA))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("çaktivizuar");
        }

        @Test
        @DisplayName("role default CLIENT kur nuk specifikohet")
        void defaultRole_isClient() {
            RegisterRequest req = new RegisterRequest(
                    "c@test.com", "Pass1234", "C", "D", null, "slug", null);
            when(userRepository.existsByEmail(any())).thenReturn(false);
            TenantCompany tenant = activeTenant(1L, "slug");
            when(tenantRepository.findBySlug("slug")).thenReturn(Optional.of(tenant));
            when(passwordEncoder.encode(any())).thenReturn("enc");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            User saved = activeUser(1L, tenant);
            when(userRepository.save(captor.capture())).thenReturn(saved);
            when(roleRepository.findByName(any())).thenReturn(Optional.empty());
            when(provisioningService.provisionIfNeeded(any())).thenReturn(SCHEMA);
            stubTokenGeneration();

            service.register(req, IP, UA);

            assertThat(captor.getValue().getRole()).isEqualTo(Role.CLIENT);
        }
    }

    // ── login() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("sukses — kthen AuthResponse me tokena")
        void success() {
            TenantCompany tenant = activeTenant(1L, "slug");
            User user = activeUser(10L, tenant);
            LoginRequest req = new LoginRequest("user@test.com", "raw-pass");

            when(userRepository.findActiveByEmail("user@test.com"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("raw-pass", "encoded-pass")).thenReturn(true);
            when(provisioningService.provisionIfNeeded(tenant)).thenReturn(SCHEMA);
            stubTokenGeneration();

            AuthResponse resp = service.login(req, IP, UA);

            assertThat(resp.accessToken()).isEqualTo(ACCESS);
            assertThat(resp.userId()).isEqualTo(10L);
            verify(refreshTokenRepository).save(any());
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur user nuk gjendet")
        void throws_whenUserNotFound() {
            when(userRepository.findActiveByEmail(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login(
                    new LoginRequest("x@x.com", "pass"), IP, UA))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Kredenciale");
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur fjalëkalimi është i gabuar")
        void throws_whenWrongPassword() {
            TenantCompany tenant = activeTenant(1L, "slug");
            User user = activeUser(10L, tenant);
            when(userRepository.findActiveByEmail(any())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.login(
                    new LoginRequest("user@test.com", "wrong"), IP, UA))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur tenant është i çaktivizuar")
        void throws_whenTenantInactive() {
            TenantCompany tenant = activeTenant(1L, "slug");
            tenant.setIsActive(false);
            User user = activeUser(10L, tenant);
            when(userRepository.findActiveByEmail(any())).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> service.login(
                    new LoginRequest("user@test.com", "pass"), IP, UA))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("çaktivizuar");
        }
    }

    // ── refresh() ────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("sukses — kthen access token të ri")
        void success() {
            TenantCompany tenant = activeTenant(1L, "slug");
            User user = activeUser(10L, tenant);

            RefreshToken stored = RefreshToken.builder()
                    .user(user).token(REFRESH).revoked(false)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            when(jwtUtil.isTokenValid(REFRESH)).thenReturn(true);
            when(jwtUtil.isRefreshToken(REFRESH)).thenReturn(true);
            when(refreshTokenRepository.findByToken(REFRESH)).thenReturn(Optional.of(stored));

            TenantSchemaRegistry reg = new TenantSchemaRegistry();
            reg.setSchemaName(SCHEMA);
            when(schemaRegistryRepository.findByTenant_Id(1L)).thenReturn(Optional.of(reg));
            when(jwtUtil.generateAccessToken(any(), any(), any(), any(), any())).thenReturn(ACCESS);

            RefreshResponse resp = service.refresh(new RefreshRequest(REFRESH));
            assertThat(resp.accessToken()).isEqualTo(ACCESS);
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur token është i pavlefshëm")
        void throws_whenTokenInvalid() {
            when(jwtUtil.isTokenValid(any())).thenReturn(false);

            assertThatThrownBy(() -> service.refresh(new RefreshRequest("bad-token")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur token nuk është refresh token")
        void throws_whenNotRefreshToken() {
            when(jwtUtil.isTokenValid(any())).thenReturn(true);
            when(jwtUtil.isRefreshToken(any())).thenReturn(false);

            assertThatThrownBy(() -> service.refresh(new RefreshRequest("access-token")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur token është revoked")
        void throws_whenTokenRevoked() {
            TenantCompany tenant = activeTenant(1L, "slug");
            User user = activeUser(10L, tenant);

            RefreshToken revoked = RefreshToken.builder()
                    .user(user).token(REFRESH).revoked(true)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            when(jwtUtil.isTokenValid(REFRESH)).thenReturn(true);
            when(jwtUtil.isRefreshToken(REFRESH)).thenReturn(true);
            when(refreshTokenRepository.findByToken(REFRESH)).thenReturn(Optional.of(revoked));

            assertThatThrownBy(() -> service.refresh(new RefreshRequest(REFRESH)))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("pavlefshëm");
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur token është i skaduar")
        void throws_whenTokenExpired() {
            TenantCompany tenant = activeTenant(1L, "slug");
            User user = activeUser(10L, tenant);

            RefreshToken expired = RefreshToken.builder()
                    .user(user).token(REFRESH).revoked(false)
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();

            when(jwtUtil.isTokenValid(REFRESH)).thenReturn(true);
            when(jwtUtil.isRefreshToken(REFRESH)).thenReturn(true);
            when(refreshTokenRepository.findByToken(REFRESH)).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> service.refresh(new RefreshRequest(REFRESH)))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    // ── logout() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("sukses — vendos revoked=true")
        void success() {
            TenantCompany tenant = activeTenant(1L, "slug");
            User user = activeUser(10L, tenant);
            RefreshToken rt = RefreshToken.builder()
                    .user(user).token(REFRESH).revoked(false)
                    .expiresAt(LocalDateTime.now().plusDays(1))
                    .build();

            when(refreshTokenRepository.findByToken(REFRESH)).thenReturn(Optional.of(rt));

            service.logout(REFRESH);

            assertThat(rt.getRevoked()).isTrue();
            verify(refreshTokenRepository).save(rt);
        }

        @Test
        @DisplayName("nuk hedh exception kur token nuk ekziston")
        void noException_whenTokenNotFound() {
            when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() -> service.logout("unknown-token"));
            verify(refreshTokenRepository, never()).save(any());
        }
    }
}