package com.realestate.backend.service;

import com.realestate.backend.entity.tenant.TenantCompany;
import com.realestate.backend.entity.tenant.TenantSchemaRegistry;
import com.realestate.backend.repository.SchemaRegistryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        String schemaName = generateSchemaName(tenant);

        log.info("Duke krijuar schema '{}' për tenant '{}'", schemaName, tenant.getSlug());

        try {
            // 1. CREATE SCHEMA
            entityManager.createNativeQuery(
                    String.format("CREATE SCHEMA IF NOT EXISTS \"%s\"", schemaName)
            ).executeUpdate();

            // 2. Regjistro në registry (is_provisioned = false gjatë krijimit)
            TenantSchemaRegistry registry = new TenantSchemaRegistry();
            registry.setTenant(tenant);
            registry.setSchemaName(schemaName);
            registry.setIsProvisioned(false);
            schemaRegistryRepository.save(registry);

            // 3. Krijo të gjitha tabelat
            createTenantTables(schemaName);

            // 4. Shëno si të provizionuar
            schemaRegistryRepository.markAsProvisioned(tenant.getId(), LocalDateTime.now());

            log.info("Schema '{}' u krijua me sukses — 22 tabela + indekse", schemaName);
            return schemaName;

        } catch (Exception ex) {
            log.error("Provizionimi i '{}' dështoi: {}", schemaName, ex.getMessage(), ex);
            throw new RuntimeException("Provizionimi i skemasë dështoi: " + ex.getMessage(), ex);
        }
    }

    // ════════════════════════════════════════════════════════════
    // TABELAT PER-TENANT — saktësisht si në SQL schema
    // Renditja: prej-tabelave që nuk kanë FK → deri tek ato me FK
    // ════════════════════════════════════════════════════════════
    private void createTenantTables(String s) {

        // ────────────────────────────────────────────────────────
        // 1. addresses
        // ────────────────────────────────────────────────────────
        exec(s, """
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
            """);

        exec(s, """
            CREATE INDEX IF NOT EXISTS "idx_%s_addresses_city"
            ON "%s".addresses(city)
            """);

        exec(s, """
            CREATE INDEX IF NOT EXISTS "idx_%s_addresses_country"
            ON "%s".addresses(country)
            """);

        // ────────────────────────────────────────────────────────
        // 2. properties
        // FK: public.users(id), addresses(id)
        // search_vector: GENERATED ALWAYS AS STORED
        // ────────────────────────────────────────────────────────
        exec(s, """
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
                search_vector   tsvector GENERATED ALWAYS AS (
                                    to_tsvector('english',
                                        coalesce(title, '')       || ' ' ||
                                        coalesce(description, '') || ' ' ||
                                        coalesce(type, '')
                                    )
                                ) STORED,
                is_featured     BOOLEAN DEFAULT FALSE,
                view_count      INTEGER DEFAULT 0,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at      TIMESTAMP,
                deleted_at      TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_agent\"    ON \"%s\".properties(agent_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_type\"     ON \"%s\".properties(type)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_status\"   ON \"%s\".properties(status)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_listing\"  ON \"%s\".properties(listing_type)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_price\"    ON \"%s\".properties(price)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_area\"     ON \"%s\".properties(area_sqm)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_bedrooms\" ON \"%s\".properties(bedrooms)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_properties_fts\"      ON \"%s\".properties USING GIN(search_vector)");

        exec(s, """
            CREATE INDEX IF NOT EXISTS "idx_%s_properties_active"
            ON "%s".properties(type, bedrooms, price)
            WHERE status = 'AVAILABLE' AND deleted_at IS NULL
            """);

        // ────────────────────────────────────────────────────────
        // 3. property_price_history
        // FK: properties(id), public.users(id)
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".property_price_history (
                id          BIGSERIAL PRIMARY KEY,
                property_id BIGINT       NOT NULL REFERENCES "%s".properties(id) ON DELETE CASCADE,
                old_price   DECIMAL(12,2),
                new_price   DECIMAL(12,2) NOT NULL,
                currency    VARCHAR(10)  DEFAULT 'EUR',
                changed_by  BIGINT REFERENCES public.users(id),
                reason      VARCHAR(255),
                changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_price_history_property\" ON \"%s\".property_price_history(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_price_history_date\"     ON \"%s\".property_price_history(changed_at)");

        // ────────────────────────────────────────────────────────
        // 4. property_features
        // FK: properties(id)
        // UNIQUE: (property_id, feature)
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".property_features (
                id          BIGSERIAL PRIMARY KEY,
                property_id BIGINT      NOT NULL REFERENCES "%s".properties(id) ON DELETE CASCADE,
                feature     VARCHAR(100) NOT NULL
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_features_property\" ON \"%s\".property_features(property_id)");

        exec(s, """
            CREATE UNIQUE INDEX IF NOT EXISTS "idx_%s_features_unique"
            ON "%s".property_features(property_id, feature)
            """);

        // ────────────────────────────────────────────────────────
        // 5. property_images
        // FK: properties(id)
        // UNIQUE partial: vetëm një primary image per property
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".property_images (
                id          BIGSERIAL PRIMARY KEY,
                property_id BIGINT       NOT NULL REFERENCES "%s".properties(id) ON DELETE CASCADE,
                image_url   VARCHAR(500) NOT NULL,
                caption     VARCHAR(255),
                sort_order  INTEGER DEFAULT 0,
                is_primary  BOOLEAN DEFAULT FALSE
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_images_property\" ON \"%s\".property_images(property_id)");

        exec(s, """
            CREATE UNIQUE INDEX IF NOT EXISTS "idx_%s_one_primary_image"
            ON "%s".property_images(property_id)
            WHERE is_primary = TRUE
            """);

        // ────────────────────────────────────────────────────────
        // 6. property_views — Analytics
        // FK: properties(id), public.users(id) — user_id nullable (anonimë)
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".property_views (
                id          BIGSERIAL PRIMARY KEY,
                property_id BIGINT    NOT NULL REFERENCES "%s".properties(id) ON DELETE CASCADE,
                user_id     BIGINT    REFERENCES public.users(id),
                ip_address  VARCHAR(45),
                user_agent  TEXT,
                viewed_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_views_property\" ON \"%s\".property_views(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_views_user\"     ON \"%s\".property_views(user_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_views_date\"     ON \"%s\".property_views(viewed_at)");

        // ────────────────────────────────────────────────────────
        // 7. saved_properties — Wishlist / Favorites
        // FK: public.users(id), properties(id)
        // UNIQUE: (user_id, property_id)
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".saved_properties (
                id          BIGSERIAL PRIMARY KEY,
                user_id     BIGINT    NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
                property_id BIGINT    NOT NULL REFERENCES "%s".properties(id) ON DELETE CASCADE,
                note        TEXT,
                saved_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(user_id, property_id)
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_saved_user\"     ON \"%s\".saved_properties(user_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_saved_property\" ON \"%s\".saved_properties(property_id)");

        // ────────────────────────────────────────────────────────
        // 8. rental_listings
        // FK: properties(id), public.users(id)
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".rental_listings (
                id               BIGSERIAL PRIMARY KEY,
                property_id      BIGINT       NOT NULL REFERENCES "%s".properties(id),
                agent_id         BIGINT REFERENCES public.users(id),
                title            VARCHAR(255),
                description      TEXT,
                available_from   DATE,
                available_until  DATE,
                price            DECIMAL(12,2) CHECK (price >= 0),
                currency         VARCHAR(10)  DEFAULT 'EUR',
                deposit          DECIMAL(12,2) CHECK (deposit >= 0),
                price_period     VARCHAR(20)  DEFAULT 'MONTHLY'
                                 CHECK (price_period IN ('DAILY','WEEKLY','MONTHLY','YEARLY')),
                min_lease_months INTEGER DEFAULT 12,
                status           VARCHAR(20)  DEFAULT 'ACTIVE'
                                 CHECK (status IN ('ACTIVE','INACTIVE','EXPIRED','RENTED')),
                created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at       TIMESTAMP,
                deleted_at       TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_rental_property\" ON \"%s\".rental_listings(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_rental_status\"   ON \"%s\".rental_listings(status)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_rental_price\"    ON \"%s\".rental_listings(price)");

        // ────────────────────────────────────────────────────────
        // 9. rental_applications
        // FK: rental_listings(id), public.users(id) x2
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".rental_applications (
                id               BIGSERIAL PRIMARY KEY,
                listing_id       BIGINT REFERENCES "%s".rental_listings(id) ON DELETE CASCADE,
                client_id        BIGINT REFERENCES public.users(id),
                status           VARCHAR(20)  DEFAULT 'PENDING'
                                 CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
                message          TEXT,
                income           DECIMAL(12,2),
                move_in_date     DATE,
                reviewed_by      BIGINT REFERENCES public.users(id),
                reviewed_at      TIMESTAMP,
                rejection_reason TEXT,
                created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_rental_app_listing\" ON \"%s\".rental_applications(listing_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_rental_app_client\"  ON \"%s\".rental_applications(client_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_rental_app_status\"  ON \"%s\".rental_applications(status)");

        // ────────────────────────────────────────────────────────
        // 10. lease_contracts
        // FK: properties(id), rental_listings(id), public.users(id) x2
        // ────────────────────────────────────────────────────────
        exec(s, """
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
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_lease_property\" ON \"%s\".lease_contracts(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_lease_client\"   ON \"%s\".lease_contracts(client_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_lease_status\"   ON \"%s\".lease_contracts(status)");

        // ────────────────────────────────────────────────────────
        // 11. payments
        // FK: lease_contracts(id)
        // Partial index: pagesat e vonuara
        // ────────────────────────────────────────────────────────
        exec(s, """
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
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_payments_contract\" ON \"%s\".payments(contract_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_payments_status\"   ON \"%s\".payments(status)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_payments_due_date\" ON \"%s\".payments(due_date)");

        exec(s, """
            CREATE INDEX IF NOT EXISTS "idx_%s_payments_overdue"
            ON "%s".payments(due_date)
            WHERE status IN ('PENDING','OVERDUE')
            """);

        // ────────────────────────────────────────────────────────
        // 12. sale_listings
        // FK: properties(id), public.users(id)
        // ────────────────────────────────────────────────────────
        exec(s, """
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
                updated_at  TIMESTAMP,
                deleted_at  TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_listing_property\" ON \"%s\".sale_listings(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_listing_price\"    ON \"%s\".sale_listings(price)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_listing_status\"   ON \"%s\".sale_listings(status)");

        // ────────────────────────────────────────────────────────
        // 13. sale_contracts
        // FK: properties(id), sale_listings(id), public.users(id) x2
        // ────────────────────────────────────────────────────────
        exec(s, """
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
                created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at        TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_contract_property\" ON \"%s\".sale_contracts(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_contract_buyer\"    ON \"%s\".sale_contracts(buyer_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_contract_status\"   ON \"%s\".sale_contracts(status)");

        // ────────────────────────────────────────────────────────
        // 14. sale_payments
        // FK: sale_contracts(id)
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".sale_payments (
                id              BIGSERIAL PRIMARY KEY,
                contract_id     BIGINT REFERENCES "%s".sale_contracts(id),
                amount          DECIMAL(12,2) CHECK (amount >= 0),
                currency        VARCHAR(10)  DEFAULT 'EUR',
                payment_type    VARCHAR(30)  DEFAULT 'FULL'
                                CHECK (payment_type IN ('DEPOSIT','INSTALLMENT','FULL','COMMISSION')),
                paid_date       DATE,
                payment_method  VARCHAR(50),
                transaction_ref VARCHAR(255),
                status          VARCHAR(20)
                                CHECK (status IN ('PENDING','PAID','FAILED','REFUNDED')),
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_pay_contract\" ON \"%s\".sale_payments(contract_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_sale_pay_status\"   ON \"%s\".sale_payments(status)");

        // ────────────────────────────────────────────────────────
        // 15. agent_profiles
        // FK: public.users(id) UNIQUE — një profil për user
        // rating: BETWEEN 0 AND 5
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".agent_profiles (
                id               BIGSERIAL PRIMARY KEY,
                user_id          BIGINT UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
                phone            VARCHAR(30),
                license          VARCHAR(100),
                bio              TEXT,
                experience_years INTEGER,
                specialization   VARCHAR(100),
                photo_url        VARCHAR(500),
                rating           DECIMAL(3,2) DEFAULT 0.00 CHECK (rating BETWEEN 0 AND 5),
                total_reviews    INTEGER DEFAULT 0
            )
            """);

        // ────────────────────────────────────────────────────────
        // 16. client_profiles
        // FK: public.users(id) UNIQUE
        // preferred_contact: EMAIL | PHONE | WHATSAPP
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".client_profiles (
                id                BIGSERIAL PRIMARY KEY,
                user_id           BIGINT UNIQUE REFERENCES public.users(id) ON DELETE CASCADE,
                phone             VARCHAR(30),
                preferred_contact VARCHAR(20) DEFAULT 'EMAIL'
                                  CHECK (preferred_contact IN ('EMAIL','PHONE','WHATSAPP')),
                budget_min        DECIMAL(12,2),
                budget_max        DECIMAL(12,2),
                preferred_type    VARCHAR(50),
                preferred_city    VARCHAR(100),
                photo_url         VARCHAR(500)
            )
            """);

        // ────────────────────────────────────────────────────────
        // 17. property_lead_requests
        // FK: public.users(id) x2, properties(id)
        // source: WEBSITE | PHONE | EMAIL | REFERRAL | SOCIAL
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".property_lead_requests (
                id                BIGSERIAL PRIMARY KEY,
                client_id         BIGINT REFERENCES public.users(id),
                assigned_agent_id BIGINT REFERENCES public.users(id),
                property_id       BIGINT REFERENCES "%s".properties(id),
                type              VARCHAR(20) CHECK (type IN ('SELL','BUY','RENT','VALUATION')),
                message           TEXT,
                budget            DECIMAL(12,2),
                preferred_date    DATE,
                source            VARCHAR(50) DEFAULT 'WEBSITE'
                                  CHECK (source IN ('WEBSITE','PHONE','EMAIL','REFERRAL','SOCIAL')),
                status            VARCHAR(20) DEFAULT 'NEW'
                                  CHECK (status IN ('NEW','IN_PROGRESS','DONE','REJECTED')),
                created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at        TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_leads_agent\"  ON \"%s\".property_lead_requests(assigned_agent_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_leads_status\" ON \"%s\".property_lead_requests(status)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_leads_client\" ON \"%s\".property_lead_requests(client_id)");

        // ────────────────────────────────────────────────────────
        // 18. maintenance_requests
        // FK: properties(id), lease_contracts(id), public.users(id) x2
        // category: PLUMBING | ELECTRICAL | HVAC | STRUCTURAL | CLEANING | OTHER
        // priority: LOW | MEDIUM | HIGH | URGENT
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".maintenance_requests (
                id              BIGSERIAL PRIMARY KEY,
                property_id     BIGINT REFERENCES "%s".properties(id),
                lease_id        BIGINT REFERENCES "%s".lease_contracts(id),
                requested_by    BIGINT REFERENCES public.users(id),
                assigned_to     BIGINT REFERENCES public.users(id),
                title           VARCHAR(255),
                description     TEXT,
                category        VARCHAR(50)
                                CHECK (category IN ('PLUMBING','ELECTRICAL','HVAC','STRUCTURAL','CLEANING','OTHER')),
                priority        VARCHAR(20) DEFAULT 'MEDIUM'
                                CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
                status          VARCHAR(20) DEFAULT 'OPEN'
                                CHECK (status IN ('OPEN','IN_PROGRESS','COMPLETED','CANCELLED')),
                estimated_cost  DECIMAL(10,2),
                actual_cost     DECIMAL(10,2),
                completed_at    TIMESTAMP,
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at      TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_maintenance_property\" ON \"%s\".maintenance_requests(property_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_maintenance_status\"   ON \"%s\".maintenance_requests(status)");

        // ────────────────────────────────────────────────────────
        // 19. notifications
        // FK: public.users(id)
        // type: INFO | WARNING | SUCCESS | ERROR | REMINDER
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".notifications (
                id                  BIGSERIAL PRIMARY KEY,
                user_id             BIGINT REFERENCES public.users(id),
                title               VARCHAR(255),
                message             TEXT,
                type                VARCHAR(50) DEFAULT 'INFO'
                                    CHECK (type IN ('INFO','WARNING','SUCCESS','ERROR','REMINDER')),
                related_entity_type VARCHAR(100),
                related_entity_id   BIGINT,
                action_url          VARCHAR(500),
                is_read             BOOLEAN DEFAULT FALSE,
                read_at             TIMESTAMP,
                created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_notif_user\"    ON \"%s\".notifications(user_id)");
        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_notif_is_read\" ON \"%s\".notifications(user_id, is_read)");

        // ────────────────────────────────────────────────────────
        // 20. ai_conversations
        // FK: public.users(id)
        // context: GENERAL | PROPERTY_SEARCH | VALUATION | SUPPORT
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".ai_conversations (
                id           BIGSERIAL PRIMARY KEY,
                user_id      BIGINT REFERENCES public.users(id),
                title        VARCHAR(255),
                context      VARCHAR(100) DEFAULT 'GENERAL'
                             CHECK (context IN ('GENERAL','PROPERTY_SEARCH','VALUATION','SUPPORT')),
                total_tokens INTEGER DEFAULT 0,
                created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at   TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_ai_conv_user\" ON \"%s\".ai_conversations(user_id)");

        // ────────────────────────────────────────────────────────
        // 21. ai_messages
        // FK: ai_conversations(id) ON DELETE CASCADE
        // role: user | assistant | system
        // ────────────────────────────────────────────────────────
        exec(s, """
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
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_ai_messages_conv\" ON \"%s\".ai_messages(conversation_id)");

        // ────────────────────────────────────────────────────────
        // 22. cache_keys — Redis invalidation tracking
        // ────────────────────────────────────────────────────────
        exec(s, """
            CREATE TABLE IF NOT EXISTS "%s".cache_keys (
                id             BIGSERIAL PRIMARY KEY,
                cache_key      VARCHAR(255) NOT NULL UNIQUE,
                entity_type    VARCHAR(100),
                entity_id      BIGINT,
                ttl_seconds    INTEGER,
                invalidated_at TIMESTAMP,
                created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        exec(s, "CREATE INDEX IF NOT EXISTS \"idx_%s_cache_entity\" ON \"%s\".cache_keys(entity_type, entity_id)");

        log.debug("Tabelat e skemës '{}' u krijuan me sukses (22 tabela)", s);
    }

    // ════════════════════════════════════════════════════════════
    // HELPER: zëvendëso %s me schema name dhe ekzekuto
    //
    // SQL-et me 2 kolona (tabelë + FK) kanë dy %s:
    //   - i pari  = emri i tabelës/indeksit
    //   - i dyti  = emri i skemës në FK/ON
    // SQL-et me 3 kolona (lease_contracts, sale_contracts, maintenance)
    //   kanë tre %s.
    // Metodë e vetme exec() e menaxhon të gjitha rastet duke
    // numëruar %s në SQL dhe duke përdorur args vararg.
    // ════════════════════════════════════════════════════════════
    private void exec(String schema, String sql) {
        // Numëro sa %s ka SQL-i dhe plotëso automatikisht me schema
        long count = sql.chars().filter(c -> c == '%').count();
        Object[] args = new Object[(int) count];
        for (int i = 0; i < count; i++) args[i] = schema;

        String finalSql = String.format(sql, args);
        entityManager.createNativeQuery(finalSql).executeUpdate();
    }

    // ── Gjenero emrin e skemasë ───────────────────────────────────
    // "acme-inc" + id=1 → "tenant_acme_inc_1"
    // ID garantëson unicitët edhe nëse slug ndryshon
    private String generateSchemaName(TenantCompany tenant) {
        String base = tenant.getSlug()
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "_");
        return "tenant_" + base + "_" + tenant.getId();
    }
}