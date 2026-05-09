package com.realestate.backend.service;

import com.realestate.backend.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PermissionAdminService {

    private final PermissionRepository permissionRepository;

    public List<Map<String, Object>> getRoles() throws SQLException {
        return permissionRepository.findAllRoles();
    }

    public List<Map<String, Object>> getPermissions() throws SQLException {
        return permissionRepository.findAllPermissions();
    }

    public List<Map<String, Object>> getRolePermissions(String roleName) throws SQLException {
        return permissionRepository.findPermissionsByRole(roleName);
    }

    public void grantPermission(String roleName, String permissionName) throws SQLException {
        permissionRepository.grantPermission(roleName, permissionName);
    }

    public void revokePermission(String roleName, String permissionName) throws SQLException {
        permissionRepository.revokePermission(roleName, permissionName);
    }

    public void assignRole(Long userId, String roleName) throws SQLException {
        permissionRepository.assignRole(userId, roleName);
    }

    public void removeRole(Long userId, String roleName) throws SQLException {
        permissionRepository.removeRole(userId, roleName);
    }

    public List<Map<String, Object>> getUserRoles(Long userId) throws SQLException {
        return permissionRepository.findRolesByUser(userId);
    }
}