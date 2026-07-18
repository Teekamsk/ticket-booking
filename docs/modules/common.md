# Module: `common` (Phase 0 — foundation)

Cross-cutting infrastructure every other module builds on. No business domain of its own.

## Responsibilities

- Base persistence type and JPA auditing.
- Global error handling as RFC-7807 Problem Details.
- Stateless security skeleton with role-based method security (RBAC foundation).
- Async + scheduling enablement (used by later phases).
- Money (paise) utilities.
- A public liveness probe.

## Package layout (`com.moviebooking.ticket_booking.common`)

| Package | Class | Purpose |
|---|---|---|
| `entity` | `BaseEntity` | `@MappedSuperclass`: `id` (IDENTITY) + `created_at`/`updated_at` (UTC) via `AuditingEntityListener`. |
| `exception` | `ApiException` | Abstract base: carries `HttpStatus` + machine-readable `errorCode`. Extension point for all domain exceptions. |
| `exception` | `ResourceNotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422) | Generic reusable exceptions. Module-specific ones (e.g. `SeatUnavailableException`) extend `ApiException` directly. |
| `web` | `ProblemDetails` | Factory for Problem Details (adds `errorCode` + `timestamp`); also writes bodies from the security filter chain. |
| `web` | `GlobalExceptionHandler` | `@RestControllerAdvice`: maps any `ApiException`, bean-validation failures (`400` + field errors), and a `500` fallback. |
| `web` | `PingController` / `PingResponse` | Public `GET /api/v1/ping` liveness probe (DTO response, no entity). |
| `security` | `RestAuthenticationEntryPoint` (401), `RestAccessDeniedHandler` (403) | Emit Problem Details for auth failures at the filter level. |
| `config` | `SecurityConfig` | Stateless, CSRF off, `@EnableMethodSecurity`, public paths permitted, everything else authenticated; `BCryptPasswordEncoder` bean. |
| `config` | `JpaAuditingConfig`, `AsyncConfig`, `SchedulerConfig` | `@EnableJpaAuditing`, `@EnableAsync` (+ `notificationExecutor`), `@EnableScheduling`. |
| `util` | `MoneyUtil` | Paise ↔ rupees conversion and display formatting. |

## Error contract (system-wide)

All errors are `application/problem+json` with: `status`, `title`, `detail`, `errorCode`, `timestamp`
(+ `errors[]` for validation). Status conventions: `400` validation, `401` unauthenticated,
`403` access denied, `404` not found, `409` conflict, `410` gone (expired hold),
`422` business-rule violation, `500` unexpected. A `DataIntegrityViolationException` (e.g. a
unique-key race that slips past an application pre-check) is mapped to `409` with
`errorCode: DATA_CONFLICT` as a cross-cutting safety net.

## Key decisions

- **Flyway owns the schema** (`spring.jpa.hibernate.ddl-auto=validate`). Migrations in
  `src/main/resources/db/migration`. Phase 0 ships `V1__baseline.sql` (history table only; tables
  start in Phase 1). **Spring Boot 4 requires the `spring-boot-flyway` module** for autoconfiguration.
- **Schema-per-module topology:** each module owns a Postgres schema (`auth`, `catalog`, …) in one
  database; cross-module references are by id, not FK. Full rationale in [`../DATABASE.md`](../DATABASE.md).
- **Jackson 3** (`tools.jackson.*`) is the JSON stack in Spring Boot 4 — not `com.fasterxml.jackson`.
- **Security error responses** come from two layers: URL-level auth failures (unauthenticated /
  URL-denied) are handled by the filter chain (entry point / access-denied handler), while
  **method-security `@PreAuthorize` denials surface at the MVC layer** and are mapped to `403` by
  `GlobalExceptionHandler`'s `AccessDeniedException` handler. Both produce identical Problem Details.
- RBAC is enabled here (`@EnableMethodSecurity`); the JWT authentication filter arrives in Phase 1.

## Configuration

- `application.yaml` — dev datasource (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, default local Postgres
  `ticket_booking`), Flyway on, health actuator.
- `src/test/resources/application.yaml` — points at `ticket_booking_test`.
- Datasource is env-overridable (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`, `TEST_DB_URL`).

## How to test

- **Unit/web:** `./gradlew test` — `GlobalExceptionHandlerTest` (error contract, standalone MockMvc),
  `MoneyUtilTest`, `SecurityIntegrationTest` (full-context: ping public `200`, protected `401`),
  `TicketBookingApplicationTests` (context loads). Requires local Postgres `ticket_booking_test`.
- **Manual/API:** run `./gradlew bootRun`, then Postman folder
  *Phase 0 - Common*: `GET /api/v1/ping` → `200 UP`; `GET /actuator/health` → `UP`;
  `GET /api/v1/secure-probe` → `401` Problem Details.

## Local database setup (one-time)

```
createdb ticket_booking && createdb ticket_booking_test
psql -d postgres -c "CREATE ROLE ticket_booking LOGIN PASSWORD 'ticket_booking';"
psql -d postgres -c "ALTER DATABASE ticket_booking OWNER TO ticket_booking;"
psql -d postgres -c "ALTER DATABASE ticket_booking_test OWNER TO ticket_booking;"
```
