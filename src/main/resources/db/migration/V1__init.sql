-- ============================================================
-- PJESA 1: SCHEMA PUBLIC — Tabela globale
-- ============================================================

-- --------------------------------------------------------
-- 1. tenants_company
--    Regjistri qendror i të gjitha kompanive
-- --------------------------------------------------------
CREATE TABLE public.tenants_company (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    slug        VARCHAR(63)  NOT NULL UNIQUE,
    -- slug = schema_name suffix, max 63 chars (PostgreSQL identifier limit)
    -- p.sh. "acme-inc" → schema "tenant_acme_inc"

    plan        VARCHAR(20)  DEFAULT 'FREE'
                CHECK (plan IN ('FREE','BASIC','PRO','ENTERPRISE')),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

-- --------------------------------------------------------
-- 2. tenant_schema_registry
--    Middleware e konsulton këtë për SET search_path
--    TABELA E RE — kritike për separate schema strategy
-- --------------------------------------------------------
CREATE TABLE public.tenant_schema_registry (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL REFERENCES public.tenants_company(id) ON DELETE CASCADE,
    schema_name VARCHAR(63) NOT NULL UNIQUE,
    -- e.g. 'tenant_acme_inc', 'tenant_beta_corp'
    -- krijohet automatikisht: 'tenant_' || slug (me _ në vend të -)

    is_provisioned  BOOLEAN DEFAULT FALSE,
    -- FALSE = schema u krijua por tabelat nuk janë gati ende
    -- TRUE  = gati për përdorim

    provisioned_at  TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schema_registry_tenant ON public.tenant_schema_registry(tenant_id);
CREATE INDEX idx_schema_registry_name   ON public.tenant_schema_registry(schema_name);

-- --------------------------------------------------------
-- 3. users
--    Global — një user mund të jetë në disa tenantë
--    tenant_id MBETET këtu — nevojitet për login routing
-- --------------------------------------------------------
CREATE TABLE public.users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,

    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','AGENT','CLIENT')),
    tenant_id   BIGINT       NOT NULL REFERENCES public.tenants_company(id) ON DELETE CASCADE,
    -- tenant_id MBETET në public.users:
    -- middleware ka nevojë ta dijë cilës skemë i përket useri

    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_users_tenant ON public.users(tenant_id);
CREATE INDEX idx_users_role   ON public.users(role);
CREATE INDEX idx_users_email  ON public.users(email);
