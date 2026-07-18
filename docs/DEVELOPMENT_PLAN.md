# Development Plan — Movie Ticket Booking System

Module-by-module delivery. **Each phase is independently testable** (runnable APIs + passing
tests) and ships its own architecture doc (`docs/modules/<module>.md`) before the next phase
starts. Source of truth for design: [`DESIGN.md`](DESIGN.md). Binding rules: [`../CLAUDE.md`](../CLAUDE.md).

## Delivery contract (every phase)

Each phase is "done" only when all of these hold:

- Entities + persistence (with migrations, per DB decision below).
- Request/response DTOs (no entity crosses the API boundary), Bean Validation with messages.
- Repository → Service → RequestHandler → Controller wired per the layered architecture.
- Logging at boundaries; RFC-7807 error responses with correct status codes.
- Tests green: service unit tests, `@WebMvcTest` API tests, plus concurrency tests where noted.
  Tests run against a **local Postgres** (no Testcontainers) so `FOR UPDATE` locking is real.
- Runnable API check: extend the committed **Postman collection** for the module's endpoints,
  runnable against the app started via `./gradlew bootRun` (local Postgres).
- New/changed schema ships as a **Flyway migration** (versioned, source of truth over ddl-auto).
- `docs/modules/<module>.md` created/updated.
- `./gradlew build` and `./gradlew test` pass.

## Dependency graph (drives the order)

```
common ──▶ auth ──▶ catalog ──▶ show ──▶ booking ──▶ payment
                          │        ▲         │
             refund-policy┘        │    discount (checkout)
                                   │         │
                        refund-processing ◀──┘
                                   │
                            notification (async, listens on booking/payment events)
```

Two ordering tensions to resolve (see **Open Decisions**):
- `Show` references `RefundPolicy` → refund-policy *config* is needed before `show`.
- Booking checkout applies discounts → `discount` is needed before booking's checkout step.

---

## Phase 0 — `common` / foundation ✅ DONE

**Goal:** project skeleton every other module builds on. Delivered: `common` module (BaseEntity,
Problem-Details error handling, stateless SecurityConfig + RBAC method security, Async/Scheduler
configs, MoneyUtil, ping probe), Flyway `V1__baseline`, dev/test datasources, Postman baseline,
`docs/modules/common.md`. All tests green; app verified end-to-end. Notes for later phases:
Spring Boot 4 needs the `spring-boot-flyway` module for autoconfig; JSON stack is Jackson 3
(`tools.jackson.*`); we run against local Postgres via `bootRun`.
- `BaseEntity` (id, created_at, updated_at), auditing config.
- `GlobalExceptionHandler` (`@RestControllerAdvice`, RFC-7807 Problem Details), base domain
  exceptions (`SeatUnavailableException`, `HoldExpiredException`, `NotFoundException`, …).
- `SecurityConfig` skeleton (stateless JWT filter chain, method-security enabled for RBAC),
  `AsyncConfig`, scheduler config, money/paise utils.
- DB config, **Flyway** migration setup, profiles (dev/test), Postman collection baseline,
  OpenAPI/Swagger (optional but recommended).

**Test:** context loads; error handler returns a well-formed Problem Details for a probe endpoint;
Flyway migrations apply cleanly; `./gradlew bootRun` brings the app online against local Postgres.

## Phase 1 — `auth` ✅ DONE

Delivered: `User`/`Role` + `users` migration with admin seed, JWT infra in `common/security`
(JwtService/filter/AuthenticatedUser), register/login/me endpoints with DTOs + validation, RBAC
via `@PreAuthorize` + method-security `AccessDeniedException` → 403 mapping, `docs/modules/auth.md`,
Postman *Phase 1 - Auth*. 24 tests green; verified end-to-end (register→login→/me, seeded admin,
customer 403 / admin 200). Decisions: JWT infra in `common`; first admin via Flyway seed; `/auth/me`
added beyond DESIGN.

**Owns:** `User`. **APIs:** `POST /auth/register`, `POST /auth/login` (JWT).
- Stateless JWT **access token only** (no refresh/logout for v1), `JwtService`, BCrypt hashing.
- **RBAC:** role enum (ADMIN/CUSTOMER); method-level security (`@PreAuthorize`) enforced here and
  consumed by all later admin/customer endpoints. This is the RBAC foundation for the whole system.

**Test:** register + login happy paths; duplicate email `409`; bad credentials `401`; validation
messages; JWT issued and accepted by a protected probe endpoint; **RBAC — customer token gets `403`
on an admin route, admin token passes.**

## Phase 2 — `catalog` ✅ DONE

Delivered: City/Theatre/Screen/Seat/Movie entities + `V3` migration, admin CRUD
(`AdminCatalogController`, RBAC) + public browse (`CatalogController`), row-block bulk seat creation
with `total_seats` upkeep, per-entity services/handlers/DTOs, `CatalogMapper`, `docs/modules/catalog.md`,
Postman *Phase 2 - Catalog*. Added `MALFORMED_REQUEST`/`NoResourceFound` handlers. 46 tests green;
verified end-to-end (admin chain, public browse, RBAC, validation, invalid-enum, 404, soft-delete
hiding). Decisions: row-block seats, no-cascade soft delete, `cityId` movie filter deferred to Phase 4.

**Owns:** `City`, `Theatre`, `Screen`, `Seat`, `Movie`.
- Admin CRUD (`/admin/...`) + public browse (`/cities`, `/movies?cityId=&q=`).
- Bulk seat creation (`/admin/screens/{id}/seats`).

**Test:** admin CRUD with role enforcement (`403` for customer); uniqueness constraints; public
browse/search; soft-delete (`is_active`).

## Phase 3 — `refund-policy` (config only) ✅ DONE

Delivered: `refund` schema + `V4` migration (`refund_policies`, `refund_rules`), `RefundPolicy`/
`RefundRule` entities, admin CRUD (`AdminRefundPolicyController`, RBAC), `RefundRuleEvaluator`
(largest-threshold-≤-gap matching), `docs/modules/refund.md`, Postman *Phase 3 - Refund Policies*.
61 tests green; verified end-to-end (CRUD, rule replacement reusing a threshold, duplicate name `409`,
duplicate threshold `422`, validation, RBAC). Decisions: PUT replaces the full rule set; ≥1 rule
required; unique thresholds per policy; no DELETE (per DESIGN).

**Owns:** `RefundPolicy`, `RefundRule` (admin config). *Refund processing deferred to Phase 8.*
- `POST/PUT/GET /admin/refund-policies`, rule evaluation logic (match largest
  `min_minutes_before_show ≤ gap`).

**Test:** policy + rules CRUD; rule-matching unit tests across boundaries (1440/120/30 → %).
*(Ordering of this phase depends on Open Decision #2.)*

## Phase 4 — `show` ✅ DONE

Delivered: `show` schema + `V5` migration, `Show`/`ShowPricing`/`ShowSeat` with a **full denormalized
read-model**, cross-module read facade `catalog.api.CatalogQueryService` (+ view records) and
`RefundPolicyService.requireActive`, admin create/update/cancel + public search/detail/seat-map,
overlap + pricing-coverage guards, `ShowMapper` (group-by-theatre), `docs/modules/show.md`, Postman
*Phase 4 - Shows*. Added `MISSING_PARAMETER`/`INVALID_PARAMETER` handlers. 76 tests green; verified
end-to-end (create with derived end_time + snapshot, seat generation, search/detail/seat-map, update,
cancel, overlap `409`, coverage `422`, RBAC). Decisions: full denormalized read-model; limited PUT
(pricing + start_time while all seats AVAILABLE).

**Owns:** `Show`, `ShowPricing`, `ShowSeat`.
- Create show → validates screen-time overlap, sets per-seat-type pricing, references a refund
  policy, **generates ShowSeat rows**.
- Public search (`/shows?cityId=&movieId=&date=`), details, seat map with live availability.

**Test:** show creation generates correct ShowSeats + pricing; overlap rejection; seat-map
availability reflects AVAILABLE/HELD/BOOKED; search grouping.

## Phase 5 — `discount`

**Owns:** `Discount`, `DiscountRedemption`.
- Admin promo CRUD; `DiscountService` validate + compute (`DiscountStrategy` / `PromoCodeStrategy`).

**Test:** PERCENT with cap, FLAT, min-order eligibility, validity window, usage limits
(total & per-user); invalid/expired code messaging.

## Phase 6 — `booking` (core, concurrency-critical)

**Owns:** `SeatHold`, `Booking`, `Ticket`, `Cancellation`.
- `POST /holds` (lock ShowSeat rows `FOR UPDATE`, ordered; TTL; price snapshot; `409` on conflict).
- `GET /holds/{id}/checkout?discountCode=` (uses `discount`).
- `POST /bookings` (PENDING_PAYMENT, freeze amounts), booking history/detail.
- `POST /bookings/{id}/cancel` (policy gap check; wires to refund in Phase 8).
- `HoldExpirySweeper` (@Scheduled) + lazy expiry.

**Test (mandatory concurrency):** N threads booking the same seat → exactly one succeeds, rest
`409`; expired-hold `410`; sweeper releases seats; final ShowSeat state consistent.

## Phase 7 — `payment`

**Owns:** `Payment`.
- `POST /payments` (simulated delay + configurable failure rate, idempotency key).
- SUCCESS ⇒ booking CONFIRMED, tickets created, ShowSeats BOOKED, hold CONVERTED, publish event.
- FAILED ⇒ booking stays PENDING_PAYMENT until hold expiry.

**Test:** idempotent retries (same key → one effect); success confirms + flips seats; failure
path; payment status endpoint.

## Phase 8 — `refund` (processing)

**Owns:** `Refund` (+ uses Cancellation from booking, RefundPolicy from Phase 3).
- On cancellation: evaluate matched rule %, create `Refund` against original `Payment`, simulate processing.

**Test:** refund amount = matched-rule % of paid; cutoff rule (0%); refund against correct payment;
status transitions.

## Phase 9 — `notification`

**Owns:** `Notification`.
- `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` listeners on booking/payment/refund events.
- `LoggingNotificationSender`; `ReminderScheduler` (@Scheduled).

**Test:** events produce persisted notifications AFTER_COMMIT (none on rollback); async doesn't
block the booking flow; reminder scheduling.

---

## Resolved Decisions

1. **Database & schema:** PostgreSQL with **Flyway** versioned migrations as the schema source of
   truth (not `ddl-auto`). **No Testcontainers** for now — tests run against a local Postgres, which
   still exercises real `FOR UPDATE` locking for the booking concurrency tests.
2. **Refund-policy vs show ordering:** build **refund-policy config (Phase 3) before `show`**;
   refund *processing* stays in Phase 8. `Show.refund_policy_id` references a real policy.
3. **API verification:** committed **Postman collection** run against the app started via
   `./gradlew bootRun` (local Postgres), alongside the automated unit/web tests.
4. **Auth:** stateless **JWT access token only** (no refresh/logout in v1) + **RBAC** for
   ADMIN/CUSTOMER via method-level security. RBAC foundation lands in Phase 1 and is consumed by
   every later admin/customer endpoint.
5. **DB topology (reviewed after Phase 2):** one physical database, **schema-per-module** (`auth`,
   `catalog`, …), cross-module references by id (no cross-schema FKs). Preserves ACID + `FOR UPDATE`
   for the booking core while enabling a future service split. Rationale + service map in
   [`DATABASE.md`](DATABASE.md). All `@Entity` set `@Table(schema=…)`; each module migration creates
   its schema.

## Execution

Plan is finalized. We execute **one phase at a time**, in order (Phase 0 → 9). A phase is not
started until the previous one is green (build + tests pass, APIs runnable, module doc updated).