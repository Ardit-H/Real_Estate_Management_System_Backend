package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.User;
import com.realestate.backend.entity.enums.Role;
import com.realestate.backend.exception.ConflictException;
import com.realestate.backend.exception.ForbiddenException;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.exception.UnauthorizedException;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Lista e userëve të tenant-it ──────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsersInTenant() {
        assertIsAdmin();
        return userRepository.findAllByTenantId(TenantContext.getTenantId())
                .stream().map(this::toResponse).toList();
    }

    // ── Merr user sipas ID ────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findUser(id);
        assertSameUserOrAdmin(user);
        return toResponse(user);
    }

    // ── Profili im ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return toResponse(findUser(TenantContext.getUserId()));
    }

    // ── Ndrysho profilin tim ──────────────────────────────────
    @Transactional
    public UserResponse updateMyProfile(UserUpdateRequest req) {
        User user = findUser(TenantContext.getUserId());

        if (req.email() != null && !req.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.email())) {
                throw new ConflictException("Email ekziston tashmë: " + req.email());
            }
            user.setEmail(req.email());
        }
        if (req.firstName() != null) user.setFirstName(req.firstName());
        if (req.lastName()  != null) user.setLastName(req.lastName());

        return toResponse(userRepository.save(user));
    }

    // ── Ndrysho fjalëkalimin ──────────────────────────────────
    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = findUser(TenantContext.getUserId());

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Fjalëkalimi aktual është i gabuar");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        log.info("Fjalëkalimi u ndryshua për user id={}", user.getId());
    }

    // ── ADMIN: aktivo/çaktivizo user ──────────────────────────
    @Transactional
    public UserResponse setUserActive(Long id, UserStatusRequest req) {
        assertIsAdmin();
        User user = findUser(id);
        user.setIsActive(req.isActive());
        log.info("User id={} u {}aktivizua", id, req.isActive() ? "" : "ç");
        return toResponse(userRepository.save(user));
    }

    // ── ADMIN: ndrysho rolin ──────────────────────────────────
    @Transactional
    public UserResponse changeRole(Long id, UserRoleRequest req) {
        assertIsAdmin();
        User user = findUser(id);

        // Parandalon ADMIN-in e vetëm nga ndryshimi i rolit të tij
        if (user.getId().equals(TenantContext.getUserId())) {
            throw new ConflictException("Nuk mund të ndryshoni rolin tuaj");
        }

        user.setRole(req.role());
        log.info("Roli i user id={} u ndryshua në {}", id, req.role());
        return toResponse(userRepository.save(user));
    }

    // ── ADMIN: soft delete ────────────────────────────────────
    @Transactional
    public void deleteUser(Long id) {
        assertIsAdmin();
        User user = findUser(id);

        if (user.getId().equals(TenantContext.getUserId())) {
            throw new ConflictException("Nuk mund të fshini llogarinë tuaj");
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);
        log.info("User id={} u fshi (soft delete)", id);
    }

    // ── Helpers ───────────────────────────────────────────────

    private User findUser(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User nuk u gjet: " + id));
    }

    private void assertIsAdmin() {
        if (!TenantContext.hasRole("ADMIN")) {
            throw new ForbiddenException("Vetëm ADMIN mund të kryejë këtë veprim");
        }
    }

    private void assertSameUserOrAdmin(User user) {
        if (TenantContext.hasRole("ADMIN")) return;
        if (!user.getId().equals(TenantContext.getUserId())) {
            throw new ForbiddenException("Nuk keni leje të shikoni këtë profil");
        }
    }

    // ── Mapper ────────────────────────────────────────────────

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(),
                u.getFirstName(), u.getLastName(),
                u.getRole().name(),
                u.getTenant() != null ? u.getTenant().getId() : null,
                u.getIsActive(), u.getCreatedAt()
        );
    }
}
