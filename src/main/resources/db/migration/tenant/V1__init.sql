
CREATE TABLE IF NOT EXISTS addresses (
    id        BIGSERIAL PRIMARY KEY,
    street    VARCHAR(255),
    city      VARCHAR(100),
    state     VARCHAR(100),
    country   VARCHAR(100),
    zip_code  VARCHAR(20),
    latitude  DECIMAL(10,8),
    longitude DECIMAL(11,8)
);
CREATE INDEX IF NOT EXISTS idx_addresses_city    ON addresses(city);
CREATE INDEX IF NOT EXISTS idx_addresses_country ON addresses(country);

CREATE TABLE IF NOT EXISTS properties (
    id            BIGSERIAL    PRIMARY KEY,
    agent_id      BIGINT,                   -- ref public.users — pa FK
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    type          VARCHAR(50)  NOT NULL,
    status        VARCHAR(20)  DEFAULT 'AVAILABLE'
                  CHECK (status IN ('AVAILABLE','PENDING','SOLD','RENTED','INACTIVE')),
    listing_type  VARCHAR(10)  DEFAULT 'SALE'
                  CHECK (listing_type IN ('SALE','RENT','BOTH')),
    bedrooms      INTEGER      CHECK (bedrooms >= 0),
    bathrooms     INTEGER      CHECK (bathrooms >= 0),
    area_sqm      DECIMAL(10,2) CHECK (area_sqm >= 0),
    floor         INTEGER,
    total_floors  INTEGER,
    year_built    INTEGER,
    price         DECIMAL(12,2) CHECK (price >= 0),
    currency      VARCHAR(10)  DEFAULT 'EUR',
    price_per_sqm DECIMAL(10,2),
    address_id    BIGINT REFERENCES addresses(id),  -- brenda skemës — OK
    search_vector tsvector GENERATED ALWAYS AS (
                      to_tsvector('english',
                          coalesce(title,'') || ' ' ||
                          coalesce(description,'') || ' ' ||
                          coalesce(type,''))
                  ) STORED,
    is_featured   BOOLEAN   DEFAULT FALSE,
    view_count    INTEGER   DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,
    deleted_at    TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_properties_agent    ON properties(agent_id);
CREATE INDEX IF NOT EXISTS idx_properties_type     ON properties(type);
CREATE INDEX IF NOT EXISTS idx_properties_status   ON properties(status);
CREATE INDEX IF NOT EXISTS idx_properties_listing  ON properties(listing_type);
CREATE INDEX IF NOT EXISTS idx_properties_price    ON properties(price);
CREATE INDEX IF NOT EXISTS idx_properties_area     ON properties(area_sqm);
CREATE INDEX IF NOT EXISTS idx_properties_bedrooms ON properties(bedrooms);
CREATE INDEX IF NOT EXISTS idx_properties_fts      ON properties USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_properties_active   ON properties(type, bedrooms, price)
    WHERE status = 'AVAILABLE' AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS property_price_history (
    id          BIGSERIAL PRIMARY KEY,
    property_id BIGINT    NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    old_price   DECIMAL(12,2),
    new_price   DECIMAL(12,2) NOT NULL,
    currency    VARCHAR(10)  DEFAULT 'EUR',
    changed_by  BIGINT,                     -- ref public.users — pa FK
    reason      VARCHAR(255),
    changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_price_history_property ON property_price_history(property_id);
CREATE INDEX IF NOT EXISTS idx_price_history_date     ON property_price_history(changed_at);

CREATE TABLE IF NOT EXISTS property_features (
    id          BIGSERIAL PRIMARY KEY,
    property_id BIGINT      NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    feature     VARCHAR(100) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_features_property ON property_features(property_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_features_unique ON property_features(property_id, feature);

CREATE TABLE IF NOT EXISTS property_images (
    id          BIGSERIAL PRIMARY KEY,
    property_id BIGINT       NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    image_url   VARCHAR(500) NOT NULL,
    caption     VARCHAR(255),
    sort_order  INTEGER DEFAULT 0,
    is_primary  BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_images_property ON property_images(property_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_one_primary_image
    ON property_images(property_id) WHERE is_primary = TRUE;

CREATE TABLE IF NOT EXISTS property_views (
    id          BIGSERIAL PRIMARY KEY,
    property_id BIGINT    NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    user_id     BIGINT,                     -- ref public.users — pa FK
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    viewed_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_views_property ON property_views(property_id);
CREATE INDEX IF NOT EXISTS idx_views_user     ON property_views(user_id);
CREATE INDEX IF NOT EXISTS idx_views_date     ON property_views(viewed_at);

CREATE TABLE IF NOT EXISTS saved_properties (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL,         -- ref public.users — pa FK
    property_id BIGINT    NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
    note        TEXT,
    saved_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, property_id)
);
CREATE INDEX IF NOT EXISTS idx_saved_user     ON saved_properties(user_id);
CREATE INDEX IF NOT EXISTS idx_saved_property ON saved_properties(property_id);

CREATE TABLE IF NOT EXISTS rental_listings (
    id               BIGSERIAL PRIMARY KEY,
    property_id      BIGINT    NOT NULL REFERENCES properties(id),
    agent_id         BIGINT,               -- ref public.users — pa FK
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
);
CREATE INDEX IF NOT EXISTS idx_rental_property ON rental_listings(property_id);
CREATE INDEX IF NOT EXISTS idx_rental_status   ON rental_listings(status);
CREATE INDEX IF NOT EXISTS idx_rental_price    ON rental_listings(price);

CREATE TABLE IF NOT EXISTS rental_applications (
    id               BIGSERIAL PRIMARY KEY,
    listing_id       BIGINT REFERENCES rental_listings(id) ON DELETE CASCADE,
    client_id        BIGINT,               -- ref public.users — pa FK
    status           VARCHAR(20) DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    message          TEXT,
    income           DECIMAL(12,2),
    move_in_date     DATE,
    reviewed_by      BIGINT,               -- ref public.users — pa FK
    reviewed_at      TIMESTAMP,
    rejection_reason TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_rental_app_listing ON rental_applications(listing_id);
CREATE INDEX IF NOT EXISTS idx_rental_app_client  ON rental_applications(client_id);
CREATE INDEX IF NOT EXISTS idx_rental_app_status  ON rental_applications(status);

CREATE TABLE IF NOT EXISTS lease_contracts (
    id                BIGSERIAL PRIMARY KEY,
    property_id       BIGINT REFERENCES properties(id),
    listing_id        BIGINT REFERENCES rental_listings(id),
    client_id         BIGINT,              -- ref public.users — pa FK
    agent_id          BIGINT,              -- ref public.users — pa FK
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
);
CREATE INDEX IF NOT EXISTS idx_lease_property ON lease_contracts(property_id);
CREATE INDEX IF NOT EXISTS idx_lease_client   ON lease_contracts(client_id);
CREATE INDEX IF NOT EXISTS idx_lease_status   ON lease_contracts(status);

CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL PRIMARY KEY,
    contract_id     BIGINT REFERENCES lease_contracts(id),
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
);
CREATE INDEX IF NOT EXISTS idx_payments_contract ON payments(contract_id);
CREATE INDEX IF NOT EXISTS idx_payments_status   ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_due_date ON payments(due_date);
CREATE INDEX IF NOT EXISTS idx_payments_overdue  ON payments(due_date)
    WHERE status IN ('PENDING','OVERDUE');

CREATE TABLE IF NOT EXISTS sale_listings (
    id          BIGSERIAL PRIMARY KEY,
    property_id BIGINT REFERENCES properties(id),
    agent_id    BIGINT,                    -- ref public.users — pa FK
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
);
CREATE INDEX IF NOT EXISTS idx_sale_listing_property ON sale_listings(property_id);
CREATE INDEX IF NOT EXISTS idx_sale_listing_price    ON sale_listings(price);
CREATE INDEX IF NOT EXISTS idx_sale_listing_status   ON sale_listings(status);

CREATE TABLE IF NOT EXISTS sale_contracts (
    id                BIGSERIAL PRIMARY KEY,
    property_id       BIGINT REFERENCES properties(id),
    listing_id        BIGINT REFERENCES sale_listings(id),
    buyer_id          BIGINT,              -- ref public.users — pa FK
    agent_id          BIGINT,              -- ref public.users — pa FK
    sale_price        DECIMAL(12,2) CHECK (sale_price >= 0),
    currency          VARCHAR(10)  DEFAULT 'EUR',
    contract_date     DATE,
    handover_date     DATE,
    contract_file_url VARCHAR(500),
    status            VARCHAR(20)  DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING','COMPLETED','CANCELLED')),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sale_contract_property ON sale_contracts(property_id);
CREATE INDEX IF NOT EXISTS idx_sale_contract_buyer    ON sale_contracts(buyer_id);
CREATE INDEX IF NOT EXISTS idx_sale_contract_status   ON sale_contracts(status);

CREATE TABLE IF NOT EXISTS sale_payments (
    id              BIGSERIAL PRIMARY KEY,
    contract_id     BIGINT REFERENCES sale_contracts(id),
    amount          DECIMAL(12,2) CHECK (amount >= 0),
    currency        VARCHAR(10)  DEFAULT 'EUR',
    payment_type    VARCHAR(30)  DEFAULT 'FULL'
                    CHECK (payment_type IN ('DEPOSIT','INSTALLMENT','FULL','COMMISSION')),
    paid_date       DATE,
    payment_method  VARCHAR(50),
    transaction_ref VARCHAR(255),
    status          VARCHAR(20)  CHECK (status IN ('PENDING','PAID','FAILED','REFUNDED')),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sale_pay_contract ON sale_payments(contract_id);
CREATE INDEX IF NOT EXISTS idx_sale_pay_status   ON sale_payments(status);

CREATE TABLE IF NOT EXISTS agent_profiles (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT UNIQUE,        -- ref public.users — pa FK
    phone            VARCHAR(30),
    license          VARCHAR(100),
    bio              TEXT,
    experience_years INTEGER,
    specialization   VARCHAR(100),
    photo_url        VARCHAR(500),
    rating           DECIMAL(3,2) DEFAULT 0.00 CHECK (rating BETWEEN 0 AND 5),
    total_reviews    INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS client_profiles (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT UNIQUE,       -- ref public.users — pa FK
    phone             VARCHAR(30),
    preferred_contact VARCHAR(20) DEFAULT 'EMAIL'
                      CHECK (preferred_contact IN ('EMAIL','PHONE','WHATSAPP')),
    budget_min        DECIMAL(12,2),
    budget_max        DECIMAL(12,2),
    preferred_type    VARCHAR(50),
    preferred_city    VARCHAR(100),
    photo_url         VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS property_lead_requests (
    id                BIGSERIAL PRIMARY KEY,
    client_id         BIGINT,              -- ref public.users — pa FK
    assigned_agent_id BIGINT,              -- ref public.users — pa FK
    property_id       BIGINT REFERENCES properties(id),
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
);
CREATE INDEX IF NOT EXISTS idx_leads_agent  ON property_lead_requests(assigned_agent_id);
CREATE INDEX IF NOT EXISTS idx_leads_status ON property_lead_requests(status);
CREATE INDEX IF NOT EXISTS idx_leads_client ON property_lead_requests(client_id);

CREATE TABLE IF NOT EXISTS maintenance_requests (
    id             BIGSERIAL PRIMARY KEY,
    property_id    BIGINT REFERENCES properties(id),
    lease_id       BIGINT REFERENCES lease_contracts(id),
    requested_by   BIGINT,                -- ref public.users — pa FK
    assigned_to    BIGINT,                -- ref public.users — pa FK
    title          VARCHAR(255),
    description    TEXT,
    category       VARCHAR(50)
                   CHECK (category IN ('PLUMBING','ELECTRICAL','HVAC','STRUCTURAL','CLEANING','OTHER')),
    priority       VARCHAR(20) DEFAULT 'MEDIUM'
                   CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    status         VARCHAR(20) DEFAULT 'OPEN'
                   CHECK (status IN ('OPEN','IN_PROGRESS','COMPLETED','CANCELLED')),
    estimated_cost DECIMAL(10,2),
    actual_cost    DECIMAL(10,2),
    completed_at   TIMESTAMP,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_maintenance_property ON maintenance_requests(property_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_status   ON maintenance_requests(status);

CREATE TABLE IF NOT EXISTS notifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT,            -- ref public.users — pa FK
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
);
CREATE INDEX IF NOT EXISTS idx_notif_user    ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notif_is_read ON notifications(user_id, is_read);

CREATE TABLE IF NOT EXISTS ai_conversations (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT,                  -- ref public.users — pa FK
    title        VARCHAR(255),
    context      VARCHAR(100) DEFAULT 'GENERAL'
                 CHECK (context IN ('GENERAL','PROPERTY_SEARCH','VALUATION','SUPPORT')),
    total_tokens INTEGER DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_conv_user ON ai_conversations(user_id);

CREATE TABLE IF NOT EXISTS ai_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT    NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('user','assistant','system')),
    content         TEXT NOT NULL,
    tokens_used     INTEGER DEFAULT 0,
    model           VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conv ON ai_messages(conversation_id);

CREATE TABLE IF NOT EXISTS cache_keys (
    id             BIGSERIAL PRIMARY KEY,
    cache_key      VARCHAR(255) NOT NULL UNIQUE,
    entity_type    VARCHAR(100),
    entity_id      BIGINT,
    ttl_seconds    INTEGER,
    invalidated_at TIMESTAMP,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_cache_entity ON cache_keys(entity_type, entity_id);