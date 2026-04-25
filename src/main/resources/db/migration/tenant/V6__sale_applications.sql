-- V6__sale_applications.sql
-- Tabela për aplikimet e blerjes nga klientët

CREATE TABLE IF NOT EXISTS sale_applications (
                                                 id                    BIGSERIAL PRIMARY KEY,
                                                 listing_id            BIGINT REFERENCES sale_listings(id),
    property_id           BIGINT REFERENCES properties(id),
    buyer_id              BIGINT NOT NULL,
    agent_id              BIGINT,
    message               TEXT,
    offer_price           NUMERIC(12, 2),
    desired_purchase_date DATE,
    monthly_income        NUMERIC(12, 2),
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    rejection_reason      TEXT,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
    );

-- Indexes
CREATE INDEX idx_sale_app_listing  ON sale_applications(listing_id);
CREATE INDEX idx_sale_app_buyer    ON sale_applications(buyer_id);
CREATE INDEX idx_sale_app_status   ON sale_applications(status);
CREATE INDEX idx_sale_app_property ON sale_applications(property_id);