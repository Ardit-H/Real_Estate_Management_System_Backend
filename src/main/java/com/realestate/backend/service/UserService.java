package com.realestate.backend.service;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.entity.User;
import com.realestate.backend.exception.*;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Vlera të lejuara — reflektojnë CHECK constraints në DB ───────────────
    // role VARCHAR(20) CHECK (role IN ('ADMIN','AGENT','CLIENT'))
    private static final Set<String> VALID_ROLES = Set.of("ADMIN", "AGENT", "CLIENT");

    // ── Lista e userëve të tenant-it (ADMIN) ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsersInTenant() {
        assertIsAdmin();
        return userRepository.findAllByTenantId(TenantContext.getTenantId())
                .stream().map(this::toResponse).toList();
    }

    // ── Merr user sipas ID ────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = findUser(id);
        assertSameUserOrAdmin(user);
        return toResponse(user);
    }

    // ── Profili im ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        return toResponse(findUser(TenantContext.getUserId()));
    }

    // ── Ndrysho profilin tim ──────────────────────────────────────────────────
    @Transactional
    public UserResponse updateMyProfile(UserUpdateRequest req) {
        User user = findUser(TenantContext.getUserId());

        // ── Validime ─────────────────────────────────────────────────────────
        if (req.firstName() != null && req.firstName().isBlank()) {
            throw new BadRequestException("Emri nuk mund të jetë bosh");
        }
        if (req.lastName() != null && req.lastName().isBlank()) {
            throw new BadRequestException("Mbiemri nuk mund të jetë bosh");
        }
        if (req.firstName() != null && req.firstName().length() > 50) {
            throw new BadRequestException("Emri nuk mund të jetë më i gjatë se 50 karaktere");
        }
        if (req.lastName() != null && req.lastName().length() > 50) {
            throw new BadRequestException("Mbiemri nuk mund të jetë më i gjatë se 50 karaktere");
        }
        if (req.email() != null) {
            if (!req.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new BadRequestException("Formati i email-it është i pavlefshëm");
            }
            if (req.email().length() > 150) {
                throw new BadRequestException("Email nuk mund të jetë më i gjatë se 150 karaktere");
            }
            if (!req.email().equals(user.getEmail()) && userRepository.existsByEmail(req.email())) {
                throw new ConflictException("Email ekziston tashmë: " + req.email());
            }
        }

        if (req.email()     != null) user.setEmail(req.email().trim().toLowerCase());
        if (req.firstName() != null) user.setFirstName(req.firstName().trim());
        if (req.lastName()  != null) user.setLastName(req.lastName().trim());

        return toResponse(userRepository.save(user));
    }

    // ── Ndrysho fjalëkalimin ──────────────────────────────────────────────────
    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = findUser(TenantContext.getUserId());

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new UnauthorizedException("Fjalëkalimi aktual është i gabuar");
        }

        // ── Validime ─────────────────────────────────────────────────────────
        // min 8 karaktere + shkronja + numër — konfirmuar nga @Pattern në DTO
        // por e kontrollojmë edhe këtu si shtresë shtesë
        if (req.newPassword().length() < 8) {
            throw new BadRequestException("Fjalëkalimi i ri duhet të ketë minimum 8 karaktere");
        }
        if (!req.newPassword().matches("^(?=.*[A-Za-z])(?=.*\\d).+$")) {
            throw new BadRequestException(
                    "Fjalëkalimi i ri duhet të përmbajë të paktën një shkronjë dhe një numër");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BadRequestException(
                    "Fjalëkalimi i ri nuk mund të jetë i njëjtë me fjalëkalimin aktual");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        log.info("Fjalëkalimi u ndryshua për user id={}", user.getId());
    }

    // ── ADMIN: aktivo / çaktivizo user ───────────────────────────────────────
    @Transactional
    public UserResponse setUserActive(Long id, UserStatusRequest req) {
        assertIsAdmin();

        User user = findUser(id);

        // Nuk mund të çaktivizosh veten
        if (user.getId().equals(TenantContext.getUserId()) && Boolean.FALSE.equals(req.isActive())) {
            throw new ConflictException("Nuk mund të çaktivizoni llogarinë tuaj");
        }

        user.setIsActive(req.isActive());
        log.info("User id={} u {}aktivizua nga admin id={}",
                id, req.isActive() ? "" : "ç", TenantContext.getUserId());
        return toResponse(userRepository.save(user));
    }

    // ── ADMIN: ndrysho rolin ──────────────────────────────────────────────────
    @Transactional
    public UserResponse changeRole(Long id, UserRoleRequest req) {
        assertIsAdmin();

        User user = findUser(id);

        // Nuk mund të ndryshosh rolin tënd
        if (user.getId().equals(TenantContext.getUserId())) {
            throw new ConflictException("Nuk mund të ndryshoni rolin tuaj");
        }

        // ── Validime ─────────────────────────────────────────────────────────
        // Enum Role garanton vlerat e vlefshme — por kontrollo nëse null
        if (req.role() == null) {
            throw new BadRequestException("Roli është i detyrueshëm. Vlerat: " + VALID_ROLES);
        }

        user.setRole(req.role());
        log.info("Roli i user id={} u ndryshua në {} nga admin id={}",
                id, req.role(), TenantContext.getUserId());
        return toResponse(userRepository.save(user));
    }

    // ── ADMIN: soft delete ────────────────────────────────────────────────────
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
        log.info("User id={} u fshi (soft delete) nga admin id={}", id, TenantContext.getUserId());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    // ── Mapper ────────────────────────────────────────────────────────────────

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