package com.realestate.backend.controller;

import com.realestate.backend.entity.User;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.multitenancy.TenantContext;
import com.realestate.backend.repository.SchemaRegistryRepository;
import com.realestate.backend.repository.UserRepository;
import com.realestate.backend.security.jwt.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ImpersonationController — allows ADMIN to act as another user.
 *
 * Generates a new JWT token with the target user's claims plus an
 * "impersonatedBy" field so every action is traceable back to the
 * original admin. The admin's original token is never invalidated —
 * the frontend simply swaps tokens and restores the original on exit.
 *
 * Security rules enforced:
 *   - Only ADMIN can impersonate
 *   - Cannot impersonate another ADMIN
 *   - Cannot impersonate a user from a different tenant
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/impersonate")
@RequiredArgsConstructor
@Tag(name = "Impersonation")
@SecurityRequirement(name = "BearerAuth")
public class ImpersonationController extends BaseController {

    private final UserRepository           userRepository;
    private final SchemaRegistryRepository schemaRegistryRepository;
    private final JwtUtil                  jwtUtil;

    @PostMapping("/{userId}")
    @Operation(summary = "Start impersonating a user (ADMIN only)")
    public ResponseEntity<Map<String, String>> impersonate(
            @PathVariable Long userId) {

        // Only ADMIN can impersonate
        if (!"ADMIN".equals(TenantContext.getRole())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Only ADMIN can impersonate users"));
        }

        // Find target user
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Cannot impersonate another ADMIN
        if ("ADMIN".equals(target.getRole().name())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Cannot impersonate another ADMIN"));
        }

        // Cannot impersonate cross-tenant
        if (!target.getTenant().getId().equals(TenantContext.getTenantId())) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Cannot impersonate user from a different tenant"));
        }

        // Find schema
        String schemaName = schemaRegistryRepository
                .findByTenant_Id(target.getTenant().getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseThrow(() -> new RuntimeException("Schema not found for tenant"));

        // Generate impersonation token
        String token = jwtUtil.generateImpersonationToken(
                target.getId(),
                target.getEmail(),
                target.getTenant().getId(),
                schemaName,
                target.getRole().name(),
                TenantContext.getUserId()
        );

        log.warn("IMPERSONATION STARTED — admin={} impersonating userId={}",
                TenantContext.getUserId(), userId);

        return ok(Map.of(
                "token",     token,
                "email",     target.getEmail(),
                "role",      target.getRole().name(),
                "full_name", (target.getFirstName() + " " + target.getLastName()).trim(),
                "message",   "Now acting as " + target.getEmail()
        ));
    }

    @PostMapping("/exit")
    @Operation(summary = "Exit impersonation — frontend restores original token")
    public ResponseEntity<Map<String, String>> exit() {
        log.info("IMPERSONATION EXIT — userId={}", TenantContext.getUserId());
        return ok(Map.of("message", "Impersonation exited — restore your original token"));
    }
}