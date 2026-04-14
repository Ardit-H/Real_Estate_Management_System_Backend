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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SchemaRegistryRepository schemaRegistryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SchemaProvisioningService provisioningService;

    // ================= REGISTER =================
    @Transactional
    public AuthResponse register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email ekziston");
        }

        // krijo ose gjej tenant
        TenantCompany tenant = tenantRepository.findBySlug(req.tenantSlug())
                .orElseGet(() -> {
                    TenantCompany t = new TenantCompany();
                    t.setName(req.tenantName() != null ? req.tenantName() : req.tenantSlug());
                    t.setSlug(req.tenantSlug());
                    t.setPlan("FREE");
                    t.setIsActive(true);
                    return tenantRepository.save(t);
                });

        if (!tenant.getIsActive()) {
            throw new UnauthorizedException("Tenant i çaktivizuar");
        }

        // krijo user
        User user = new User();
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());

        // ✅ ENUM ROLE
        user.setRole(
                req.role() != null
                        ? Role.valueOf(req.role().toUpperCase())
                        : Role.CLIENT
        );

        user.setTenant(tenant);
        user.setIsActive(true);

        user = userRepository.save(user);

        // krijo schema nëse nuk ekziston
        String schemaName = provisioningService.provisionIfNeeded(tenant);

        return buildAuthResponse(user, tenant, schemaName);
    }

    // ================= LOGIN =================
    @Transactional
    public AuthResponse login(LoginRequest req) {

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Kredenciale të gabuara"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Kredenciale të gabuara");
        }

        if (!user.getIsActive()) {
            throw new UnauthorizedException("User i çaktivizuar");
        }

        if (!user.getTenant().getIsActive()) {
            throw new UnauthorizedException("Tenant i çaktivizuar");
        }

        String schemaName = schemaRegistryRepository
                .findByTenant_Id(user.getTenant().getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseGet(() -> "public");

        return buildAuthResponse(user, user.getTenant(), schemaName);
    }

    // ================= REFRESH =================
    @Transactional
    public RefreshResponse refresh(RefreshRequest req) {

        if (!jwtUtil.isTokenValid(req.refreshToken()) ||
                !jwtUtil.isRefreshToken(req.refreshToken())) {
            throw new UnauthorizedException("Refresh token i pavlefshëm");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Token nuk ekziston"));

        if (stored.getRevoked() || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Token i pavlefshëm ose i skaduar");
        }

        User user = stored.getUser();

        String schemaName = schemaRegistryRepository
                .findByTenant_Id(user.getTenant().getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseThrow(() -> new UnauthorizedException("Schema nuk gjendet"));

        // ✅ ENUM → STRING për JWT
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getTenant().getId(),
                schemaName,
                user.getRole().name()
        );

        return new RefreshResponse(newAccessToken);
    }

    // ================= LOGOUT =================
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    // ================= HELPER =================
    private AuthResponse buildAuthResponse(User user,
                                           TenantCompany tenant,
                                           String schemaName) {

        String accessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                tenant.getId(),
                schemaName,
                user.getRole().name() // ✅ ENUM → STRING
        );

        String refreshToken = jwtUtil.generateRefreshToken(
                user.getId(),
                tenant.getId()
        );

        saveRefreshToken(user, refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(), // ✅ përdor metodën nga User entity
                user.getRole().name(),
                tenant.getId(),
                tenant.getName(),
                schemaName
        );
    }

    private void saveRefreshToken(User user, String token) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(token);
        rt.setRevoked(false);
        rt.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(rt);
    }
}