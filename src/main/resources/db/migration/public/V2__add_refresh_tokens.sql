-- ============================================================
-- V2__add_refresh_tokens.sql
--
-- Shton tabelën public.refresh_tokens
-- e nevojshme për AuthService (login, refresh, logout)
--
-- V1__init.sql ka: tenants_company, tenant_schema_registry, users
-- V2 shton:        refresh_tokens
-- ============================================================

CREATE TABLE public.refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL
                REFERENCES public.users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      DEFAULT FALSE,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_user
    ON public.refresh_tokens(user_id);

CREATE INDEX idx_refresh_token
    ON public.refresh_tokens(token);

CREATE INDEX idx_refresh_expires
    ON public.refresh_tokens(expires_at);

-- Partial index: vetëm token-et aktive
-- Query-t e middleware përdorin këtë index
CREATE INDEX idx_refresh_active
    ON public.refresh_tokens(user_id, expires_at)
    WHERE revoked = FALSE;