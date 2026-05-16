package com.realestate.backend.controller;

import com.realestate.backend.entity.InviteToken;
import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.exception.ResourceNotFoundException;
import com.realestate.backend.exception.UnauthorizedException;
import com.realestate.backend.repository.InviteRepository;
import com.realestate.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class InviteController {

    private final InviteRepository inviteRepository;
    private final TenantRepository tenantRepository;

    @PostMapping
    public ResponseEntity<Map<String, String>> createInvite(@RequestBody Map<String, Object> body) {

        String role     = (String) body.get("role");
        Long   tenantId = Long.valueOf(body.get("tenant_id").toString());

        TenantCompany tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nuk ekziston"));

        String token = UUID.randomUUID().toString().replace("-", ""); // 32 karaktere

        InviteToken invite = InviteToken.builder()
                .token(token)
                .tenant(tenant)
                .role(role.toUpperCase())
                .used(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        inviteRepository.save(invite);

        return ResponseEntity.ok(Map.of("token", token));
    }


    @GetMapping("/{token}")
    public ResponseEntity<Map<String, String>> verifyInvite(@PathVariable String token) {

        InviteToken invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite i pavlefshëm"));

        if (!invite.isValid()) {
            throw new UnauthorizedException("Invite ka skaduar ose është përdorur");
        }

        return ResponseEntity.ok(Map.of(
                "tenant_slug", invite.getTenant().getSlug(),
                "tenant_name", invite.getTenant().getName(),
                "role",        invite.getRole()
        ));
    }

    @PatchMapping("/{token}/use")
    public ResponseEntity<Void> markUsed(@PathVariable String token) {

        InviteToken invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invite nuk ekziston"));

        invite.setUsed(true);
        inviteRepository.save(invite);

        return ResponseEntity.noContent().build();
    }
}