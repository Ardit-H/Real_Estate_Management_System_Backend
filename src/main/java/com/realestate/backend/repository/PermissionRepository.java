package com.realestate.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PermissionRepository {

    private final DataSource dataSource;

    public List<Map<String, Object>> findAllRoles() throws SQLException {
        List<Map<String, Object>> roles = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, name, description, is_active FROM public.roles ORDER BY id"
             )) {
            while (rs.next()) {
                roles.add(Map.of(
                        "id",          rs.getLong("id"),
                        "name",        rs.getString("name"),
                        "description", rs.getString("description"),
                        "isActive",    rs.getBoolean("is_active")
                ));
            }
        }
        return roles;
    }

    public List<Map<String, Object>> findAllPermissions() throws SQLException {
        List<Map<String, Object>> permissions = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT id, name, http_method, api_path, description " +
                             "FROM public.permissions ORDER BY http_method, api_path"
             )) {
            while (rs.next()) {
                permissions.add(Map.of(
                        "id",          rs.getLong("id"),
                        "name",        rs.getString("name"),
                        "httpMethod",  rs.getString("http_method"),
                        "apiPath",     rs.getString("api_path"),
                        "description", rs.getString("description")
                ));
            }
        }
        return permissions;
    }

    public List<Map<String, Object>> findPermissionsByRole(String roleName) throws SQLException {
        List<Map<String, Object>> permissions = new ArrayList<>();
        String sql = """
            SELECT p.id, p.name, p.http_method, p.api_path, p.description
            FROM public.permissions p
            JOIN public.role_permissions rp ON rp.permission_id = p.id
            JOIN public.roles r             ON r.id = rp.role_id
            WHERE r.name = ?
            ORDER BY p.http_method, p.api_path
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    permissions.add(Map.of(
                            "id",          rs.getLong("id"),
                            "name",        rs.getString("name"),
                            "httpMethod",  rs.getString("http_method"),
                            "apiPath",     rs.getString("api_path"),
                            "description", rs.getString("description")
                    ));
                }
            }
        }
        return permissions;
    }

    public void grantPermission(String roleName, String permissionName) throws SQLException {
        String sql = """
            INSERT INTO public.role_permissions (role_id, permission_id)
            SELECT r.id, p.id
            FROM public.roles r, public.permissions p
            WHERE r.name = ? AND p.name = ?
            ON CONFLICT DO NOTHING
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            stmt.setString(2, permissionName);
            stmt.executeUpdate();
        }
    }

    public void revokePermission(String roleName, String permissionName) throws SQLException {
        String sql = """
            DELETE FROM public.role_permissions
            WHERE role_id       = (SELECT id FROM public.roles       WHERE name = ?)
              AND permission_id = (SELECT id FROM public.permissions WHERE name = ?)
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            stmt.setString(2, permissionName);
            stmt.executeUpdate();
        }
    }

    public void assignRole(Long userId, String roleName) throws SQLException {
        String sql = """
            INSERT INTO public.user_roles (user_id, role_id)
            SELECT ?, r.id FROM public.roles r WHERE r.name = ?
            ON CONFLICT DO NOTHING
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, roleName);
            stmt.executeUpdate();
        }
    }

    public void removeRole(Long userId, String roleName) throws SQLException {
        String sql = """
            DELETE FROM public.user_roles
            WHERE user_id = ?
              AND role_id = (SELECT id FROM public.roles WHERE name = ?)
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, roleName);
            stmt.executeUpdate();
        }
    }

    public List<Map<String, Object>> findRolesByUser(Long userId) throws SQLException {
        List<Map<String, Object>> roles = new ArrayList<>();
        String sql = """
            SELECT r.id, r.name, r.description
            FROM public.roles r
            JOIN public.user_roles ur ON ur.role_id = r.id
            WHERE ur.user_id = ?
        """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(Map.of(
                            "id",          rs.getLong("id"),
                            "name",        rs.getString("name"),
                            "description", rs.getString("description")
                    ));
                }
            }
        }
        return roles;
    }
}