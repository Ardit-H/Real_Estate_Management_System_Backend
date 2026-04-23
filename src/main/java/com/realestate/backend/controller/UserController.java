package com.realestate.backend.controller;

import com.realestate.backend.dto.user.UserProfileDtos.*;
import com.realestate.backend.service.AgentProfileService;
import com.realestate.backend.service.ClientProfileService;
import com.realestate.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users & Profiles")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService         userService;
    private final AgentProfileService agentProfileService;
    private final ClientProfileService clientProfileService;

    // ══════════════════ USERS ════════════════════════════════════

    @GetMapping
    @Operation(summary = "Listo të gjithë userët e tenant-it (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersInTenant());
    }

    @GetMapping("/me")
    @Operation(summary = "Profili im")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr user sipas ID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }
    @GetMapping("/agents/list")
    @Operation(summary = "Lista e agjentëve me emra (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<UserResponse>> getAgentsList() {
        return ResponseEntity.ok(userService.getAgentsInTenant()); // ← metodë e re
    }

    @PutMapping("/me")
    @Operation(summary = "Ndrysho profilin tim (emri, email)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Ndrysho fjalëkalimin tim")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Aktivo / çaktivizo user (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(userService.setUserActive(id, request));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Ndrysho rolin e userit (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fshij user (soft delete — ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════ AGENT PROFILES ═══════════════════════════

    @GetMapping("/agents")
    @Operation(summary = "Listo të gjithë agjentët me profil")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<List<AgentProfileResponse>> getAllAgents() {
        return ResponseEntity.ok(agentProfileService.getAllAgents());
    }

    @GetMapping("/agents/me")
    @Operation(summary = "Profili im si agjent")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<AgentProfileResponse> getMyAgentProfile() {
        return ResponseEntity.ok(agentProfileService.getMyProfile());
    }

    @GetMapping("/agents/{userId}")
    @Operation(summary = "Profili i agjentit sipas userId")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<AgentProfileResponse> getAgentProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(agentProfileService.getByUserId(userId));
    }

    @PutMapping("/agents/me")
    @Operation(summary = "Krijo / ndrysho profilin tim si agjent")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<AgentProfileResponse> upsertAgentProfile(
            @Valid @RequestBody AgentProfileRequest request) {
        return ResponseEntity.ok(agentProfileService.upsertMyProfile(request));
    }

    @PutMapping("/agents/{userId}")
    @Operation(summary = "Ndrysho profilin e agjentit (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgentProfileResponse> updateAgentProfile(
            @PathVariable Long userId,
            @Valid @RequestBody AgentProfileRequest request) {
        return ResponseEntity.ok(agentProfileService.updateProfile(userId, request));
    }

    // ══════════════════ CLIENT PROFILES ══════════════════════════

    @GetMapping("/clients/me")
    @Operation(summary = "Profili im si klient")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<ClientProfileResponse> getMyClientProfile() {
        return ResponseEntity.ok(clientProfileService.getMyProfile());
    }

    @GetMapping("/clients/{userId}")
    @Operation(summary = "Profili i klientit sipas userId (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<ClientProfileResponse> getClientProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(clientProfileService.getByUserId(userId));
    }

    @PutMapping("/clients/me")
    @Operation(summary = "Krijo / ndrysho profilin tim si klient")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<ClientProfileResponse> upsertClientProfile(
            @Valid @RequestBody ClientProfileRequest request) {
        return ResponseEntity.ok(clientProfileService.upsertMyProfile(request));
    }
}
