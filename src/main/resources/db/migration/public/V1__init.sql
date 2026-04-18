
CREATE TABLE public.tenants_company (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    slug        VARCHAR(63)  NOT NULL UNIQUE,


    plan        VARCHAR(20)  DEFAULT 'FREE'
                CHECK (plan IN ('FREE','BASIC','PRO','ENTERPRISE')),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);


CREATE TABLE public.tenant_schema_registry (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL REFERENCES public.tenants_company(id) ON DELETE CASCADE,
    schema_name VARCHAR(63) NOT NULL UNIQUE,


    is_provisioned  BOOLEAN DEFAULT FALSE,

    provisioned_at  TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schema_registry_tenant ON public.tenant_schema_registry(tenant_id);
CREATE INDEX idx_schema_registry_name   ON public.tenant_schema_registry(schema_name);

CREATE TABLE public.users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,

    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN','AGENT','CLIENT')),
    tenant_id   BIGINT       NOT NULL REFERENCES public.tenants_company(id) ON DELETE CASCADE,


    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    deleted_at  TIMESTAMP
);

CREATE INDEX idx_users_tenant ON public.users(tenant_id);
CREATE INDEX idx_users_role   ON public.users(role);
CREATE INDEX idx_users_email  ON public.users(email);
