package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.User;
import com.realestate.backend.entity.enums.Role;
import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Unit Tests")
class UserServiceTest {

    @Mock UserRepository  userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService service;

    private MockedStatic<TenantContext> tenantCtx;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID   = 10L;
    private static final Long ADMIN_ID  = 99L;

    @BeforeEach
    void openTenant() {
        tenantCtx = mockStatic(TenantContext.class);
    }

    @AfterEach
    void closeTenant() {
        tenantCtx.close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private User buildUser(Long id, Role role) {
        TenantCompany tenant = new TenantCompany();
        tenant.setId(TENANT_ID);
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.com");
        u.setPassword("encoded");
        u.setFirstName("Test"); u.setLastName("User");
        u.setRole(role);
        u.setTenant(tenant);
        u.setIsActive(true);
        return u;
    }

    private void asAdmin() {
        tenantCtx.when(TenantContext::getUserId).thenReturn(ADMIN_ID);
        tenantCtx.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(true);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(true);
    }

    private void asUser(Long id) {
        tenantCtx.when(TenantContext::getUserId).thenReturn(id);
        tenantCtx.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(false);
        tenantCtx.when(() -> TenantContext.hasRole("ADMIN", "AGENT")).thenReturn(false);
    }

    // ── getAllUsersInTenant() ─────────────────────────────────────

    @Nested
    @DisplayName("getAllUsersInTenant()")
    class GetAll {

        @Test
        @DisplayName("ADMIN — kthen listën e userëve")
        void admin_returnsList() {
            asAdmin();
            User u1 = buildUser(1L, Role.CLIENT);
            User u2 = buildUser(2L, Role.AGENT);
            when(userRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(u1, u2));

            List<UserResponse> result = service.getAllUsersInTenant();
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("jo-ADMIN — hedh ForbiddenException")
        void nonAdmin_throws() {
            asUser(USER_ID);
            assertThatThrownBy(() -> service.getAllUsersInTenant())
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── getById() ────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("ADMIN — shikon cilindo user")
        void admin_canViewAny() {
            asAdmin();
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            UserResponse resp = service.getById(USER_ID);
            assertThat(resp.id()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("user — shikon vetëm profilin e tij")
        void user_canViewOwn() {
            asUser(USER_ID);
            tenantCtx.when(() -> TenantContext.hasRole("ADMIN")).thenReturn(false);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThatNoException().isThrownBy(() -> service.getById(USER_ID));
        }

        @Test
        @DisplayName("user — hedh ForbiddenException kur shikon tjetrin")
        void user_cannotViewOther() {
            asUser(USER_ID);
            User other = buildUser(20L, Role.CLIENT);
            when(userRepository.findById(20L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.getById(20L))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("hedh ResourceNotFoundException kur user nuk ekziston")
        void throws_whenNotFound() {
            asAdmin();
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── updateMyProfile() ────────────────────────────────────────

    @Nested
    @DisplayName("updateMyProfile()")
    class UpdateMyProfile {

        @Test
        @DisplayName("sukses — ndrysho emrin dhe mbiemrin")
        void success_updateName() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest("Ardit", "Hoti", null);
            UserResponse resp = service.updateMyProfile(req);

            assertThat(resp.firstName()).isEqualTo("Ardit");
            assertThat(resp.lastName()).isEqualTo("Hoti");
        }

        @Test
        @DisplayName("sukses — ndrysho email-in")
        void success_updateEmail() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserUpdateRequest req = new UserUpdateRequest(null, null, "new@test.com");
            UserResponse resp = service.updateMyProfile(req);

            assertThat(resp.email()).isEqualTo("new@test.com");
        }

        @Test
        @DisplayName("hedh BadRequestException kur emri është bosh")
        void throws_whenFirstNameBlank() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> service.updateMyProfile(
                    new UserUpdateRequest("   ", null, null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Emri");
        }

        @Test
        @DisplayName("hedh BadRequestException kur emri tejkalon 50 karaktere")
        void throws_whenFirstNameTooLong() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> service.updateMyProfile(
                    new UserUpdateRequest("A".repeat(51), null, null)))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("hedh BadRequestException kur email ka format të gabuar")
        void throws_whenEmailInvalidFormat() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));

            assertThatThrownBy(() -> service.updateMyProfile(
                    new UserUpdateRequest(null, null, "not-an-email")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("email");
        }

        @Test
        @DisplayName("hedh ConflictException kur email ekziston tashmë")
        void throws_whenEmailTaken() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

            assertThatThrownBy(() -> service.updateMyProfile(
                    new UserUpdateRequest(null, null, "taken@test.com")))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ── changePassword() ─────────────────────────────────────────

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("sukses — ndrysho fjalëkalimin")
        void success() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("OldPass1", "encoded")).thenReturn(true);
            when(passwordEncoder.matches("NewPass1", "encoded")).thenReturn(false);
            when(passwordEncoder.encode("NewPass1")).thenReturn("new-encoded");
            when(userRepository.save(any())).thenReturn(u);

            assertThatNoException().isThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("OldPass1", "NewPass1")));

            assertThat(u.getPassword()).isEqualTo("new-encoded");
        }

        @Test
        @DisplayName("hedh UnauthorizedException kur fjalëkalimi aktual është i gabuar")
        void throws_whenCurrentPasswordWrong() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("wrong", "NewPass1")))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("hedh BadRequestException kur fjalëkalimi i ri ka < 8 karaktere")
        void throws_whenNewPasswordTooShort() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("OldPass1", "encoded")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("OldPass1", "Ab1")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("8");
        }

        @Test
        @DisplayName("hedh BadRequestException kur fjalëkalimi i ri nuk ka numër")
        void throws_whenNewPasswordNoDigit() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("OldPass1", "encoded")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("OldPass1", "OnlyLetters")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("shkronjë");
        }

        @Test
        @DisplayName("hedh BadRequestException kur fjalëkalimi i ri është i njëjtë me të vjetrin")
        void throws_whenSameAsOld() {
            asUser(USER_ID);
            User u = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(u));
            when(passwordEncoder.matches("OldPass1", "encoded")).thenReturn(true);
            when(passwordEncoder.matches("OldPass1", "encoded")).thenReturn(true);

            assertThatThrownBy(() ->
                    service.changePassword(new ChangePasswordRequest("OldPass1", "OldPass1")))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("njëjtë");
        }
    }

    // ── setUserActive() ──────────────────────────────────────────

    @Nested
    @DisplayName("setUserActive()")
    class SetUserActive {

        @Test
        @DisplayName("ADMIN — çaktivizo user tjetër")
        void admin_deactivatesOther() {
            asAdmin();
            User target = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse resp = service.setUserActive(USER_ID, new UserStatusRequest(false));
            assertThat(resp.isActive()).isFalse();
        }

        @Test
        @DisplayName("hedh ConflictException kur admin çaktivizon veten")
        void throws_whenAdminDeactivatesSelf() {
            asAdmin();
            User admin = buildUser(ADMIN_ID, Role.ADMIN);
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() ->
                    service.setUserActive(ADMIN_ID, new UserStatusRequest(false)))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("llogarinë tuaj");
        }

        @Test
        @DisplayName("jo-ADMIN — hedh ForbiddenException")
        void nonAdmin_throws() {
            asUser(USER_ID);
            assertThatThrownBy(() ->
                    service.setUserActive(USER_ID, new UserStatusRequest(false)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    // ── changeRole() ─────────────────────────────────────────────

    @Nested
    @DisplayName("changeRole()")
    class ChangeRole {

        @Test
        @DisplayName("ADMIN — ndrysho rolin e user tjetër")
        void admin_changesRole() {
            asAdmin();
            User target = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserResponse resp = service.changeRole(USER_ID, new UserRoleRequest(Role.AGENT));
            assertThat(resp.role()).isEqualTo("AGENT");
        }

        @Test
        @DisplayName("hedh ConflictException kur admin ndrysho rolin e vet")
        void throws_whenAdminChangesSelfRole() {
            asAdmin();
            User admin = buildUser(ADMIN_ID, Role.ADMIN);
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() ->
                    service.changeRole(ADMIN_ID, new UserRoleRequest(Role.CLIENT)))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("hedh BadRequestException kur role është null")
        void throws_whenRoleNull() {
            asAdmin();
            User target = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));

            assertThatThrownBy(() ->
                    service.changeRole(USER_ID, new UserRoleRequest(null)))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("detyrueshëm");
        }
    }

    // ── deleteUser() ─────────────────────────────────────────────

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("ADMIN — fshi (soft delete) user tjetër")
        void admin_softDeletesOther() {
            asAdmin();
            User target = buildUser(USER_ID, Role.CLIENT);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(target));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.deleteUser(USER_ID);

            assertThat(target.getDeletedAt()).isNotNull();
            assertThat(target.getIsActive()).isFalse();
        }

        @Test
        @DisplayName("hedh ConflictException kur admin fshin veten")
        void throws_whenAdminDeletesSelf() {
            asAdmin();
            User admin = buildUser(ADMIN_ID, Role.ADMIN);
            when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> service.deleteUser(ADMIN_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("llogarinë tuaj");
        }
    }

    // ── getAgentsInTenant() ──────────────────────────────────────

    @Nested
    @DisplayName("getAgentsInTenant()")
    class GetAgents {

        @Test
        @DisplayName("ADMIN — kthen vetëm agjentët")
        void admin_returnsOnlyAgents() {
            asAdmin();
            User agent  = buildUser(1L, Role.AGENT);
            User client = buildUser(2L, Role.CLIENT);
            when(userRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(List.of(agent, client));

            List<UserResponse> result = service.getAgentsInTenant();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).role()).isEqualTo("AGENT");
        }

        @Test
        @DisplayName("CLIENT — kthen vetëm agjentët e tenant-it")
        void client_returnsAgents() {
            asUser(USER_ID);
            User agent  = buildUser(1L, Role.AGENT);
            User client = buildUser(2L, Role.CLIENT);
            when(userRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(List.of(agent, client));

            List<UserResponse> result = service.getAgentsInTenant();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).role()).isEqualTo("AGENT");
        }
    }
}