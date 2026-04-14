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

    // ── Connection pa tenant (p.sh. për Flyway, startup) ────────
    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        // Reset search_path para se t'i kthehet pool-it
        resetSearchPath(connection);
        connection.close();
    }

    // ── Connection per-tenant ────────────────────────────────────
    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = dataSource.getConnection();

        // Sanitizo identifier — lejo vetëm a-z, 0-9, _
        String safeSchema = sanitize(tenantIdentifier);

        try {
            // SET LOCAL — vlen VETËM brenda transaksionit aktual.
            // Kur transaksioni mbyllet, search_path kthehet automatikisht.
            // Kjo është e sigurt me HikariCP dhe PgBouncer.
            //
            // KUJDES: SET LOCAL kërkon autoCommit=false (brenda transaksionit).
            // Nëse connection ka autoCommit=true, SET LOCAL bëhet SET global.
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
        // Commit + reset search_path para kthimit në pool
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

    /**
     * Sanitizo schema identifier.
     * Lejo vetëm a-z, 0-9, _ — parandalon SQL injection.
     * Mbështjell me thonjëza dyfishe për identifikues të sigurt.
     */
    private String sanitize(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "public";
        }
        // Hiq çdo karakter jo-alfanumerik (përveç _)
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