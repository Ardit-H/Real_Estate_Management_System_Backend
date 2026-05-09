package com.realestate.backend.multitenancy.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SchemaMultiTenantConnectionProvider — routes every DB connection to the
 * correct tenant schema by setting PostgreSQL's search_path.
 *
 * Hibernate calls this class whenever it needs a database connection.
 * Instead of returning a plain connection, this provider first executes:
 *   SET LOCAL search_path TO "{schemaName}", public
 *
 * This tells PostgreSQL to resolve all unqualified table names (e.g. "properties")
 * against the tenant schema first, then fall back to public. The result is that
 * every query Hibernate runs automatically hits the correct tenant's tables
 * without any changes to repository or service code.
 *
 * SET LOCAL is used instead of SET so the search_path change is scoped to the
 * current transaction only and resets automatically when the transaction ends,
 * preventing schema leakage between requests on the same connection.
 *
 * sanitize() strips any characters outside [a-z0-9_] before interpolating the
 * schema name into the SQL string, preventing SQL injection via the tenant
 * identifier even though it comes from a trusted JWT claim.
 *
 * releaseConnection() resets search_path to public before returning the
 * connection to the pool, so a recycled connection never carries a stale
 * tenant schema from a previous request.
 *
 * supportsAggressiveRelease() returns false because this provider manages
 * connections directly from a DataSource pool (HikariCP) rather than a JTA
 * transaction manager — aggressive release is only safe with JTA.
 */
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