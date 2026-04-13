package com.realestate.backend.service;

import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.repository.SchemaRegistryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaProvisioningService {

    private final SchemaRegistryRepository schemaRegistryRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // ── Merr ose krijo schema ────────────────────────────────────
    @Transactional
    public String provisionIfNeeded(TenantCompany tenant) {
        return schemaRegistryRepository
                .findByTenant_Id(tenant.getId())
                .map(TenantSchemaRegistry::getSchemaName)
                .orElseGet(() -> createSchemaForTenant(tenant));
    }

    // ── Krijo schema + tabela ────────────────────────────────────
    @Transactional
    public String createSchemaForTenant(TenantCompany tenant) {

        // Gjenero emrin e sigurt
        // "acme-inc" → "tenant_acme_inc_1"
        String schemaName = generateSchemaName(tenant);

        log.info("Duke krijuar schema '{}' për tenant '{}'",
                schemaName, tenant.getSlug());

        try {
            // ── 1. CREATE SCHEMA me kuota dyfishe ─────────────────
            // Kuotat janë KRITIKE — pa to dështon me slug si "acme-inc"
            entityManager.createNativeQuery(
                    String.format("CREATE SCHEMA IF NOT EXISTS \"%s\"", schemaName)
            ).executeUpdate();

            // ── 2. Regjistro në registry ───────────────────────────
            TenantSchemaRegistry registry = new TenantSchemaRegistry();
            registry.setTenant(tenant);
            registry.setSchemaName(schemaName);
            registry.setIsProvisioned(false);
            schemaRegistryRepository.save(registry);

            // ── 3. Krijo tabelat ───────────────────────────────────
            createTenantTables(schemaName);

            // ── 4. Shëno si të provizionuar ────────────────────────
            schemaRegistryRepository.markAsProvisioned(
                    tenant.getId(), LocalDateTime.now());

            log.info("Schema '{}' u krijua me sukses", schemaName);
            return schemaName;

        } catch (Exception ex) {
            log.error("Provizionimi i '{}' dështoi: {}",
                    schemaName, ex.getMessage(), ex);
            throw new RuntimeException(
                    "Provizionimi i skemasë dështoi: " + ex.getMessage(), ex);
        }
    }

    // ── Tabelat per-tenant ───────────────────────────────────────
    private void createTenantTables(String schema) {

        // Çdo tabelë me kuota dyfishe: "%s".table_name
        // Gjithashtu FK cross-schema referojnë public.users me kuota

        // addresses
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".addresses (
                id          BIGSERIAL PRIMARY KEY,
                street      VARCHAR(255),
                city        VARCHAR(100),
                state       VARCHAR(100),
                country     VARCHAR(100),
                zip_code    VARCHAR(20),
                latitude    DECIMAL(10,8),
                longitude   DECIMAL(11,8)
            )
        """, schema)).executeUpdate();

        // properties — FK tek public.users me schema explicit
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".properties (
                id              BIGSERIAL PRIMARY KEY,
                agent_id        BIGINT REFERENCES public.users(id),
                title           VARCHAR(255) NOT NULL,
                description     TEXT,
                type            VARCHAR(50)  NOT NULL,
                status          VARCHAR(20)  DEFAULT 'AVAILABLE'
                                CHECK (status IN ('AVAILABLE','PENDING','SOLD','RENTED','INACTIVE')),
                listing_type    VARCHAR(10)  DEFAULT 'SALE'
                                CHECK (listing_type IN ('SALE','RENT','BOTH')),
                bedrooms        INTEGER      CHECK (bedrooms >= 0),
                bathrooms       INTEGER      CHECK (bathrooms >= 0),
                area_sqm        DECIMAL(10,2) CHECK (area_sqm >= 0),
                floor           INTEGER,
                total_floors    INTEGER,
                year_built      INTEGER,
                price           DECIMAL(12,2) CHECK (price >= 0),
                currency        VARCHAR(10)  DEFAULT 'EUR',
                price_per_sqm   DECIMAL(10,2),
                address_id      BIGINT REFERENCES "%s".addresses(id),
                is_featured     BOOLEAN DEFAULT FALSE,
                view_count      INTEGER DEFAULT 0,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at      TIMESTAMP,
                deleted_at      TIMESTAMP
            )
        """, schema, schema)).executeUpdate();

        // property_images
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".property_images (
                id          BIGSERIAL PRIMARY KEY,
                property_id BIGINT NOT NULL REFERENCES "%s".properties(id) ON DELETE CASCADE,
                image_url   VARCHAR(500) NOT NULL,
                caption     VARCHAR(255),
                sort_order  INTEGER DEFAULT 0,
                is_primary  BOOLEAN DEFAULT FALSE
            )
        """, schema, schema)).executeUpdate();

        // Unique index: vetëm një primary image
        entityManager.createNativeQuery(String.format("""
            CREATE UNIQUE INDEX IF NOT EXISTS "idx_%s_one_primary"
            ON "%s".property_images(property_id)
            WHERE is_primary = TRUE
        """, schema, schema)).executeUpdate();

        // rental_listings
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".rental_listings (
                id               BIGSERIAL PRIMARY KEY,
                property_id      BIGINT NOT NULL REFERENCES "%s".properties(id),
                agent_id         BIGINT REFERENCES public.users(id),
                title            VARCHAR(255),
                available_from   DATE,
                price            DECIMAL(12,2) CHECK (price >= 0),
                currency         VARCHAR(10)  DEFAULT 'EUR',
                deposit          DECIMAL(12,2),
                price_period     VARCHAR(20)  DEFAULT 'MONTHLY'
                                 CHECK (price_period IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
                min_lease_months INTEGER DEFAULT 12,
                status           VARCHAR(20)  DEFAULT 'ACTIVE'
                                 CHECK (status IN ('ACTIVE','INACTIVE','EXPIRED','RENTED')),
                created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                deleted_at       TIMESTAMP
            )
        """, schema, schema)).executeUpdate();

        // lease_contracts
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".lease_contracts (
                id                BIGSERIAL PRIMARY KEY,
                property_id       BIGINT REFERENCES "%s".properties(id),
                listing_id        BIGINT REFERENCES "%s".rental_listings(id),
                client_id         BIGINT REFERENCES public.users(id),
                agent_id          BIGINT REFERENCES public.users(id),
                start_date        DATE,
                end_date          DATE,
                rent              DECIMAL(12,2) CHECK (rent >= 0),
                deposit           DECIMAL(12,2) CHECK (deposit >= 0),
                currency          VARCHAR(10)  DEFAULT 'EUR',
                contract_file_url VARCHAR(500),
                status            VARCHAR(20)  DEFAULT 'ACTIVE'
                                  CHECK (status IN ('ACTIVE','ENDED','CANCELLED','PENDING_SIGNATURE')),
                created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at        TIMESTAMP
            )
        """, schema, schema, schema)).executeUpdate();

        // payments
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".payments (
                id              BIGSERIAL PRIMARY KEY,
                contract_id     BIGINT REFERENCES "%s".lease_contracts(id),
                amount          DECIMAL(12,2) CHECK (amount >= 0),
                currency        VARCHAR(10)  DEFAULT 'EUR',
                payment_type    VARCHAR(30)  DEFAULT 'RENT'
                                CHECK (payment_type IN ('RENT','DEPOSIT','LATE_FEE','MAINTENANCE')),
                due_date        DATE,
                paid_date       DATE,
                payment_method  VARCHAR(50),
                transaction_ref VARCHAR(255),
                status          VARCHAR(20)  DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING','PAID','FAILED','OVERDUE','REFUNDED')),
                notes           TEXT,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """, schema, schema)).executeUpdate();

        // sale_listings
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".sale_listings (
                id          BIGSERIAL PRIMARY KEY,
                property_id BIGINT REFERENCES "%s".properties(id),
                agent_id    BIGINT REFERENCES public.users(id),
                price       DECIMAL(12,2) CHECK (price >= 0),
                currency    VARCHAR(10)  DEFAULT 'EUR',
                negotiable  BOOLEAN DEFAULT TRUE,
                description TEXT,
                highlights  TEXT,
                status      VARCHAR(20)  DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','SOLD','CANCELLED','PENDING')),
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                deleted_at  TIMESTAMP
            )
        """, schema, schema)).executeUpdate();

        // sale_contracts
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".sale_contracts (
                id                BIGSERIAL PRIMARY KEY,
                property_id       BIGINT REFERENCES "%s".properties(id),
                listing_id        BIGINT REFERENCES "%s".sale_listings(id),
                buyer_id          BIGINT REFERENCES public.users(id),
                agent_id          BIGINT REFERENCES public.users(id),
                sale_price        DECIMAL(12,2) CHECK (sale_price >= 0),
                currency          VARCHAR(10)  DEFAULT 'EUR',
                contract_date     DATE,
                handover_date     DATE,
                contract_file_url VARCHAR(500),
                status            VARCHAR(20)  DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','COMPLETED','CANCELLED')),
                created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """, schema, schema, schema)).executeUpdate();

        // property_lead_requests
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".property_lead_requests (
                id                  BIGSERIAL PRIMARY KEY,
                client_id           BIGINT REFERENCES public.users(id),
                assigned_agent_id   BIGINT REFERENCES public.users(id),
                property_id         BIGINT REFERENCES "%s".properties(id),
                type                VARCHAR(20) CHECK (type IN ('SELL','BUY','RENT','VALUATION')),
                message             TEXT,
                budget              DECIMAL(12,2),
                status              VARCHAR(20) DEFAULT 'NEW'
                                    CHECK (status IN ('NEW','IN_PROGRESS','DONE','REJECTED')),
                created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at          TIMESTAMP
            )
        """, schema, schema)).executeUpdate();

        // notifications
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".notifications (
                id          BIGSERIAL PRIMARY KEY,
                user_id     BIGINT REFERENCES public.users(id),
                title       VARCHAR(255),
                message     TEXT,
                type        VARCHAR(50) DEFAULT 'INFO'
                            CHECK (type IN ('INFO','WARNING','SUCCESS','ERROR','REMINDER')),
                is_read     BOOLEAN DEFAULT FALSE,
                read_at     TIMESTAMP,
                created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """, schema)).executeUpdate();

        // ai_conversations
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".ai_conversations (
                id           BIGSERIAL PRIMARY KEY,
                user_id      BIGINT REFERENCES public.users(id),
                title        VARCHAR(255),
                context      VARCHAR(100) DEFAULT 'GENERAL'
                             CHECK (context IN ('GENERAL','PROPERTY_SEARCH','VALUATION','SUPPORT')),
                total_tokens INTEGER DEFAULT 0,
                created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """, schema)).executeUpdate();

        // ai_messages
        entityManager.createNativeQuery(String.format("""
            CREATE TABLE IF NOT EXISTS "%s".ai_messages (
                id              BIGSERIAL PRIMARY KEY,
                conversation_id BIGINT NOT NULL
                                REFERENCES "%s".ai_conversations(id) ON DELETE CASCADE,
                role            VARCHAR(20) NOT NULL
                                CHECK (role IN ('user','assistant','system')),
                content         TEXT NOT NULL,
                tokens_used     INTEGER DEFAULT 0,
                model           VARCHAR(100),
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """, schema, schema)).executeUpdate();

        // ── Indekse kryesore ──────────────────────────────────────
        entityManager.createNativeQuery(String.format(
                "CREATE INDEX IF NOT EXISTS \"idx_%s_prop_status\" ON \"%s\".properties(status)",
                schema, schema)).executeUpdate();

        entityManager.createNativeQuery(String.format(
                "CREATE INDEX IF NOT EXISTS \"idx_%s_prop_price\" ON \"%s\".properties(price)",
                schema, schema)).executeUpdate();

        entityManager.createNativeQuery(String.format(
                "CREATE INDEX IF NOT EXISTS \"idx_%s_leads_status\" ON \"%s\".property_lead_requests(status)",
                schema, schema)).executeUpdate();

        log.debug("Tabelat e skemës '{}' u krijuan me sukses", schema);
    }

    // ── Gjenero emrin e skemasë ───────────────────────────────────
    private String generateSchemaName(TenantCompany tenant) {
        // "acme-inc" → "tenant_acme_inc_1"
        // Shtojmë ID për të garantuar unicitët edhe nëse slug ndryshon
        String base = tenant.getSlug()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_");
        return "tenant_" + base + "_" + tenant.getId();
    }
}