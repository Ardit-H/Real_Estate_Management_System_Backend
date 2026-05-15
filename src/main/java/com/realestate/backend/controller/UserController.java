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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users & Profiles")
@SecurityRequirement(name = "BearerAuth")
public class UserController extends BaseController {

    private final UserService          userService;
    private final AgentProfileService  agentProfileService;
    private final ClientProfileService clientProfileService;

    // ══════════════════ USERS ════════════════════════════════════

    @GetMapping
    @Operation(summary = "Listo të gjithë userët e tenant-it (ADMIN)")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ok(userService.getAllUsersInTenant());
    }

    @GetMapping("/me")
    @Operation(summary = "Profili im")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ok(userService.getMyProfile());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr user sipas ID")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ok(userService.getById(id));
    }

    @GetMapping("/agents/list")
    @Operation(summary = "Lista e agjentëve me emra (ADMIN/AGENT)")
    public ResponseEntity<List<UserResponse>> getAgentsList() {
        return ok(userService.getAgentsInTenant());
    }

    @PutMapping("/me")
    @Operation(summary = "Ndrysho profilin tim (emri, email)")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UserUpdateRequest request) {
        return ok(userService.updateMyProfile(request));
    }

    @PatchMapping("/me/password")
    @Operation(summary = "Ndrysho fjalëkalimin tim")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return noContent();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Aktivo / çaktivizo user (ADMIN)")
    public ResponseEntity<UserResponse> setStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return ok(userService.setUserActive(id, request));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Ndrysho rolin e userit (ADMIN)")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UserRoleRequest request) {
        return ok(userService.changeRole(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Fshij user (soft delete — ADMIN)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return noContent();
    }

    // ══════════════════ AGENT PROFILES ═══════════════════════════

    @GetMapping("/agents")
    @Operation(summary = "Listo të gjithë agjentët me profil")
    public ResponseEntity<List<AgentProfileResponse>> getAllAgents() {
        return ok(agentProfileService.getAllAgents());
    }

    @GetMapping("/agents/me")
    @Operation(summary = "Profili im si agjent")
    public ResponseEntity<AgentProfileResponse> getMyAgentProfile() {
        return ok(agentProfileService.getMyProfile());
    }

    @GetMapping("/agents/{userId}")
    @Operation(summary = "Profili i agjentit sipas userId")
    public ResponseEntity<AgentProfileResponse> getAgentProfile(@PathVariable Long userId) {
        return ok(agentProfileService.getByUserId(userId));
    }

    @PutMapping("/agents/me")
    @Operation(summary = "Krijo / ndrysho profilin tim si agjent")
    public ResponseEntity<AgentProfileResponse> upsertAgentProfile(
            @Valid @RequestBody AgentProfileRequest request) {
        return ok(agentProfileService.upsertMyProfile(request));
    }

    @PutMapping("/agents/{userId}")
    @Operation(summary = "Ndrysho profilin e agjentit (ADMIN)")
    public ResponseEntity<AgentProfileResponse> updateAgentProfile(
            @PathVariable Long userId,
            @Valid @RequestBody AgentProfileRequest request) {
        return ok(agentProfileService.updateProfile(userId, request));
    }

    @PatchMapping("/agents/{userId}/rate")
    @Operation(summary = "Rate an agent (CLIENT)")
    public ResponseEntity<Void> rateAgent(
            @PathVariable Long userId,
            @RequestBody Map<String, Double> body) {
        agentProfileService.addRating(userId, BigDecimal.valueOf(body.get("rating")));
        return noContent();
    }


    // ══════════════════ CLIENT PROFILES ══════════════════════════

    @GetMapping("/clients/me")
    @Operation(summary = "Profili im si klient")
    public ResponseEntity<ClientProfileResponse> getMyClientProfile() {
        return ok(clientProfileService.getMyProfile());
    }

    @GetMapping("/clients/{userId}")
    @Operation(summary = "Profili i klientit sipas userId (ADMIN/AGENT)")
    public ResponseEntity<ClientProfileResponse> getClientProfile(@PathVariable Long userId) {
        return ok(clientProfileService.getByUserId(userId));
    }

    @PutMapping("/clients/me")
    @Operation(summary = "Krijo / ndrysho profilin tim si klient")
    public ResponseEntity<ClientProfileResponse> upsertClientProfile(
            @Valid @RequestBody ClientProfileRequest request) {
        return ok(clientProfileService.upsertMyProfile(request));
    }
}