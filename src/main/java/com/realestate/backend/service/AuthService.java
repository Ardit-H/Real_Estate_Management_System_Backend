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


    @Transactional
    public AuthResponse register(RegisterRequest req,
                                 String ipAddress, String userAgent) {

        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException("Email ekziston");
        }

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

        User user = new User();
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName());
        user.setLastName(req.lastName());
        user.setRole(req.role() != null
                ? Role.valueOf(req.role().toUpperCase())
                : Role.CLIENT);
        user.setTenant(tenant);
        user.setIsActive(true);

        user = userRepository.save(user);

        String schemaName = provisioningService.provisionIfNeeded(tenant);

        return buildAuthResponse(user, tenant, schemaName, ipAddress, userAgent);
    }


    @Transactional
    public AuthResponse login(LoginRequest req,
                              String ipAddress, String userAgent) {

        User user = userRepository.findActiveByEmail(req.email())
                .orElseThrow(() -> new UnauthorizedException("Kredenciale të gabuara"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Kredenciale të gabuara");
        }

        if (!user.getTenant().getIsActive()) {
            throw new UnauthorizedException("Tenant i çaktivizuar");
        }


        String schemaName = provisioningService.provisionIfNeeded(user.getTenant());

        return buildAuthResponse(user, user.getTenant(), schemaName, ipAddress, userAgent);
    }


    @Transactional
    public RefreshResponse refresh(RefreshRequest req) {

        if (!jwtUtil.isTokenValid(req.refreshToken()) ||
                !jwtUtil.isRefreshToken(req.refreshToken())) {
            throw new UnauthorizedException("Refresh token i pavlefshëm");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Token nuk ekziston"));

        if (!stored.isValid()) {
            throw new UnauthorizedException("Token i pavlefshëm ose i skaduar");
        }

        User user = stored.getUser();

        String schemaName = schemaRegistryRepository
                .findByTenant_Id(user.getTenant().getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseThrow(() -> new UnauthorizedException("Schema nuk gjendet"));

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getTenant().getId(),
                schemaName,
                user.getRole().name()
        );

        return new RefreshResponse(newAccessToken);
    }


    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }


    private AuthResponse buildAuthResponse(User user, TenantCompany tenant,
                                           String schemaName,
                                           String ipAddress, String userAgent) {
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(),
                tenant.getId(), schemaName,
                user.getRole().name()
        );

        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), tenant.getId());
        saveRefreshToken(user, refreshToken, ipAddress, userAgent);

        return new AuthResponse(
                accessToken, refreshToken,
                user.getId(), user.getEmail(),
                user.getFullName(), user.getRole().name(),
                tenant.getId(), tenant.getName(), schemaName
        );
    }


    private void saveRefreshToken(User user, String token,
                                  String ipAddress, String userAgent) {
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(token)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        refreshTokenRepository.save(rt);
    }
}