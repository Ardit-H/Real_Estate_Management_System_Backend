package com.realestate.backend.controller;

import com.realestate.backend.service.PermissionAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Permission Management")
@SecurityRequirement(name = "BearerAuth")
public class PermissionAdminController extends BaseController {

    private final PermissionAdminService permissionAdminService;

    @GetMapping("/roles")
    @Operation(summary = "List all roles")
    public ResponseEntity<List<Map<String, Object>>> getRoles() throws SQLException {
        return ok(permissionAdminService.getRoles());
    }

    @GetMapping("/permissions")
    @Operation(summary = "List all permissions")
    public ResponseEntity<List<Map<String, Object>>> getPermissions() throws SQLException {
        return ok(permissionAdminService.getPermissions());
    }

    @GetMapping("/roles/{roleName}/permissions")
    @Operation(summary = "Get permissions for a role")
    public ResponseEntity<List<Map<String, Object>>> getRolePermissions(
            @PathVariable String roleName) throws SQLException {
        return ok(permissionAdminService.getRolePermissions(roleName));
    }

    @PostMapping("/permissions/roles/{roleName}/{permissionName}")
    @Operation(summary = "Grant a permission to a role")
    public ResponseEntity<Map<String, String>> grantPermission(
            @PathVariable String roleName,
            @PathVariable String permissionName) throws SQLException {
        permissionAdminService.grantPermission(roleName, permissionName);
        return ok(Map.of(
                "message", "Permission '" + permissionName + "' granted to role '" + roleName + "'"
        ));
    }

    @DeleteMapping("/permissions/roles/{roleName}/{permissionName}")
    @Operation(summary = "Revoke a permission from a role")
    public ResponseEntity<Map<String, String>> revokePermission(
            @PathVariable String roleName,
            @PathVariable String permissionName) throws SQLException {
        permissionAdminService.revokePermission(roleName, permissionName);
        return ok(Map.of(
                "message", "Permission '" + permissionName + "' revoked from role '" + roleName + "'"
        ));
    }

    @PostMapping("/users/{userId}/roles/{roleName}")
    @Operation(summary = "Assign a role to a user")
    public ResponseEntity<Map<String, String>> assignRole(
            @PathVariable Long   userId,
            @PathVariable String roleName) throws SQLException {
        permissionAdminService.assignRole(userId, roleName);
        return ok(Map.of(
                "message", "Role '" + roleName + "' assigned to user " + userId
        ));
    }

    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @Operation(summary = "Remove a role from a user")
    public ResponseEntity<Map<String, String>> removeRole(
            @PathVariable Long   userId,
            @PathVariable String roleName) throws SQLException {
        permissionAdminService.removeRole(userId, roleName);
        return ok(Map.of(
                "message", "Role '" + roleName + "' removed from user " + userId
        ));
    }

    @GetMapping("/users/{userId}/roles")
    @Operation(summary = "Get roles for a user")
    public ResponseEntity<List<Map<String, Object>>> getUserRoles(
            @PathVariable Long userId) throws SQLException {
        return ok(permissionAdminService.getUserRoles(userId));
    }
}