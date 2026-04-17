package com.realestate.backend.multitenancy.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Component
public class SchemaMultiTenantConnectionProvider
        implements MultiTenantConnectionProvider<String> {

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {

        resetSearchPath(connection);
        connection.close();
    }


    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();


        String safeSchema = sanitize(tenantIdentifier);

        try {

            if (connection.getAutoCommit()) {
                connection.setAutoCommit(false);
            }

            connection.createStatement().execute(
                    String.format("SET LOCAL search_path TO \"%s\", public", safeSchema)
            );

            log.debug("search_path vendosur: {} (tenant={})", safeSchema, tenantIdentifier);

        } catch (SQLException e) {
            log.error("Gabim gjatë vendosjes search_path për '{}': {}",
                    safeSchema, e.getMessage());
            connection.close();
            throw e;
        }

        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier,
                                  Connection connection) throws SQLException {

        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
            resetSearchPath(connection);
        } catch (SQLException e) {
            log.warn("Gabim gjatë release connection: {}", e.getMessage());
            try { connection.rollback(); } catch (SQLException ignored) {}
        } finally {
            connection.close();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────
    private void resetSearchPath(Connection connection) {
        try {
            connection.createStatement()
                    .execute("SET search_path TO public");
        } catch (SQLException e) {
            log.warn("Nuk u reset search_path: {}", e.getMessage());
        }
    }


    private String sanitize(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "public";
        }

        String clean = identifier.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_");
        if (clean.isEmpty()) return "public";
        return clean;
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return unwrapType.isAssignableFrom(getClass());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException("Cannot unwrap to " + unwrapType);
    }
}