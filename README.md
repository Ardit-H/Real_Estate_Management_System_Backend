# Real Estate Management System — Backend

A multi-tenant real estate management platform built with Spring Boot 3.5 and PostgreSQL. The system supports property listings, rental and sale workflows, lease contracts, payments, maintenance requests, lead management, notifications, and AI-powered features.

---

## Architecture Overview

The backend follows a layered architecture pattern with strict separation of concerns. Each layer communicates only with the layer directly below it, ensuring maintainability and testability.

```
Client (React)
      |
      | HTTP/REST
      v
Controllers  — receive and validate HTTP requests, delegate to services
Services     — contain all business logic, orchestrate data access
Repositories — data access layer using Spring Data JPA
Entities     — JPA-mapped domain models
Database     — PostgreSQL with schema-based multi-tenancy
```

### Multi-Tenancy

The system implements schema-based multi-tenancy using Hibernate's `MultiTenantConnectionProvider` interface. Each company (tenant) gets its own isolated PostgreSQL schema at registration time. The `public` schema holds shared data — users, tenants, refresh tokens. Every tenant schema holds per-company data — properties, contracts, payments, leads, notifications, and maintenance requests.

The tenant is identified from the JWT token on every request. The `JwtAuthFilter` extracts `tenantId`, `schemaName`, `userId`, and `role` from the token, sets them on `TenantContext` (a `ThreadLocal` wrapper), and Hibernate uses the schema name to route all queries to the correct schema automatically.

Schema provisioning runs via Flyway when a new tenant registers. Migrations in `db/migration/tenant` are applied to the new schema automatically. On application startup, `TenantMigrationService` runs pending migrations for all existing tenant schemas.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5 |
| Language | Java 21 |
| Database | PostgreSQL 15 |
| ORM | Hibernate / Spring Data JPA |
| Migrations | Flyway |
| Security | Spring Security + JWT (JJWT 0.12) |
| Caching | Redis (Spring Cache) |
| AI Integration | Groq API (llama-3.1-8b-instant) |
| Documentation | SpringDoc OpenAPI / Swagger UI |
| Build | Maven |


---

## Domain Model

The system is organized around these core domains:

**Properties** — The central entity. Properties have a type, status, listing type (SALE/RENT/BOTH), address, images, features, and price history. Agents create and manage properties. Full-text search is powered by PostgreSQL `tsvector` with a generated column.

**Rental Flow** — An agent creates a RentalListing for a property. Clients submit RentalApplications. When approved, the agent creates a LeaseContract. The contract generates commission Payment records automatically on activation.

**Sale Flow** — An agent creates a SaleListing. Clients submit SaleApplications. The agent creates a SaleContract. On completion, the system generates commission SalePayment records automatically based on whether the property came from a client lead (Scenario 1) or is company-owned (Scenario 2).

**Leads** — Clients submit PropertyLeadRequests expressing interest. Admins assign leads to agents. Agents accept (NEW to IN_PROGRESS), work on them, and close them (DONE or REJECTED). Agents can also decline a lead without rejecting it, which returns the lead to the unassigned pool.

**Payments and Commission** — When a LeaseContract is activated or a SaleContract is completed, the system automatically creates the correct payment records based on the commission model. Scenario 1 applies when the property was sold/rented by a client through the lead system (the original property owner gets a share). Scenario 2 applies to company-owned properties.

**Notifications** — Thirteen service triggers create notifications automatically across the system. MaintenanceService, LeaseContractService, SaleService, LeadService, and RentalService all inject NotificationService and create contextual notifications for agents, clients, and technicians at key workflow steps.

**AI Features** — Six AI-powered endpoints powered by the Groq API. Property description generation, price estimation, client chat assistant, contract summarizer, payment risk analysis, and lead-to-property matching.

---

## Authentication and Authorization

Authentication uses stateless JWT tokens. On login or register, the system returns an access token (1 hour) and a refresh token (7 days stored in the database). The `JwtAuthFilter` validates every request, extracts claims, populates `TenantContext`, and sets the Spring Security `Authentication` object.

Authorization is enforced at two levels. `SecurityConfig` defines URL-level rules by HTTP method and role. `@PreAuthorize` annotations on controller methods enforce method-level access. Roles are `ADMIN`, `AGENT`, and `CLIENT`.

The `TenantContext` class wraps a `ThreadLocal<TenantInfo>` and exposes static helpers used throughout service classes to check the current user's role and ID without injecting additional dependencies.

---

## Commission Logic

**Rental (triggered on LeaseContract PENDING_SIGNATURE to ACTIVE):**

```
Commission Total = Monthly Rent x 3%
Owner Amount     = Monthly Rent x 97%

Scenario 1 (property came from a completed lead):
  RENT             -> property owner (97% minus already-paid deposits)
  COMMISSION 50%   -> company
  AGENT_COMMISSION 40% -> agent
  CLIENT_BONUS 10% -> property owner

Scenario 2 (company-owned property):
  RENT             -> company (97% minus already-paid deposits)
  COMMISSION 60%   -> company
  AGENT_COMMISSION 40% -> agent
```

**Sale (triggered on SaleContract PENDING to COMPLETED):**

Same structure as rental but applied to the total sale price instead of monthly rent.

---

## API Overview

The API exposes over 60 REST endpoints. All endpoints require a Bearer JWT token except `/api/auth/**`.

| Module | Base Path |
|---|---|
| Authentication | /api/auth |
| Properties | /api/properties |
| Property Images | /api/properties/{id}/images |
| Rental Listings | /api/rentals/listings |
| Rental Applications | /api/rentals/applications |
| Lease Contracts | /api/contracts/lease |
| Payments | /api/payments |
| Sale Listings | /api/sales/listings |
| Sale Applications | /api/sales/applications |
| Sale Contracts | /api/sales/contracts |
| Sale Payments | /api/sales/payments |
| Leads | /api/leads |
| Maintenance | /api/maintenance |
| Notifications | /api/notifications |
| Users and Profiles | /api/users |
| Tenants | /api/admin/tenants |
| AI Features | /api/ai |

Full interactive documentation is available at `http://localhost:8080/swagger-ui.html` when the application is running.

---

## Setup and Running

### Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+

### Database Setup

```sql
CREATE USER realestate_user WITH PASSWORD 'realestate_pass';
CREATE DATABASE realestate_db OWNER realestate_user;
GRANT ALL PRIVILEGES ON DATABASE realestate_db TO realestate_user;
```

### Configuration

The application reads from `src/main/resources/application.yml`. Key configuration values:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/realestate_db
    username: realestate_user
    password: realestate_pass

jwt:
  secret: your-256-bit-secret-key-minimum-32-characters
  expiration-ms: 3600000
  refresh-expiration-ms: 604800000

groq:
  api:
    key: your-groq-api-key

app:
  upload:
    dir: ./uploads
```

### Running

```bash
mvn spring-boot:run
```

Flyway runs automatically on startup and applies all pending migrations. New tenant schemas are provisioned on first registration.

---

## Key Design Decisions

**Schema isolation over row-level security** — Each tenant gets a fully isolated PostgreSQL schema. This eliminates the risk of data leakage between tenants through query bugs and makes it straightforward to back up or delete a single tenant's data without affecting others.

**Cross-schema foreign keys via Long columns** — Entities that reference `public.users` (such as `agent_id`, `client_id`) store the ID as a plain `Long` column rather than a JPA `@ManyToOne`. This avoids Hibernate attempting to join across schema boundaries, which would fail at the connection level.

**JWT contains all routing information** — The token carries `userId`, `tenantId`, `schemaName`, and `role`. This means every request is self-contained. No database lookup is needed to identify the tenant or authorize the user at the filter level.

**Commission payments are immutable records** — When a contract completes, the system creates explicit Payment or SalePayment rows for each recipient. These records are never modified retroactively. This creates a clear audit trail of who received what and when.

**Soft deletes on core entities** — Properties, rental listings, and sale listings use a `deleted_at` timestamp column instead of hard deletes. All queries filter by `deleted_at IS NULL`. This preserves historical data and prevents orphaned references in contracts and applications.
