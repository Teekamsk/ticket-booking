# Movie Ticket Booking System

A backend for booking movie tickets at scale — multiple cities, theatres, screens, and shows with
**seat-level booking**. It supports timebound seat holds that auto-release on expiry, per-show pricing
tiers, promo discounts, a simulated payment + confirmation flow, refunds under configurable policies,
and non-blocking multi-channel notifications. Concurrent attempts to book the same seat are correctly
serialized so a seat is **never double-allocated**.

Two roles: **ADMIN** (manage cities, theatres, shows, seat layouts, pricing, discounts, refund
policies) and **CUSTOMER** (browse shows, hold/book/cancel seats, view booking history).

> Built as a **modular monolith** (package-by-module) with a **schema-per-module** database, designed
> so individual modules can later be extracted into separate services. See
> [`docs/DATABASE.md`](docs/DATABASE.md) for the rationale.

---

## Tech stack

- **Java 17**, **Spring Boot 4.1** (Spring MVC, Spring Data JPA, Spring Security, Bean Validation)
- **PostgreSQL** with **Flyway** migrations (schema is versioned, not `ddl-auto`)
- **Gradle** (Kotlin DSL), **Lombok**, **JWT** (jjwt) auth, **JaCoCo** coverage
- Testing: **JUnit 5 + Mockito + AssertJ + MockMvc** against a real local Postgres

---

## Architecture at a glance

Package-by-module modular monolith under `com.moviebooking.ticket_booking.<module>`. Each module owns
its layers and its own DB schema; cross-module calls go **service → service** (never into another
module's repository), and cross-module references are **by id** (no cross-schema foreign keys).

**Per-request layering:** `Controller → RequestHandler → Service → Repository → Entity`
- *Controller* — HTTP mapping only. *RequestHandler* — orchestration + entity↔DTO mapping + error
  mapping (one per request). *Service* — business logic + transactions. *Repository* — Spring Data JPA
  (concurrency locks live here). *Entity* — never leaves the persistence layer (APIs are DTO-only).

| Module | Owns | Responsibility |
|---|---|---|
| `auth` | User | Registration, login (JWT), roles (RBAC) |
| `catalog` | City, Theatre, Screen, Seat, Movie | Admin CRUD + public browse/search |
| `show` | Show, ShowPricing, ShowSeat | Show scheduling, pricing, seat-map; generates the ShowSeat rows booking locks |
| `discount` | Discount, DiscountRedemption | Promo-code CRUD, validate + compute |
| `booking` | SeatHold, Booking, Ticket, Cancellation | Holds (TTL), checkout, booking, confirmation, cancellation — **all seat locking** |
| `payment` | Payment | Simulated gateway with delay + idempotency |
| `refund` | RefundPolicy, RefundRule, Refund | Policy config + rule evaluation + payout on cancellation |
| `notification` | Notification | Multi-channel communication service (EMAIL/SMS/PUSH), async listeners + reminders |
| `common` | — | BaseEntity, RFC-7807 error handling, security/JWT, async/scheduler config, money utils |

**The double-booking guarantee** (the system's headline requirement) is enforced by a pessimistic
`SELECT … FOR UPDATE` lock on `show.show_seats` at hold time, plus a partial unique index on active
tickets as a final DB safety net. Peripheral, eventually-consistent work (refund payout, notifications)
happens via `@Async` `AFTER_COMMIT` domain events.

### Design & architecture docs

- [`docs/DESIGN.md`](docs/DESIGN.md) — **the authoritative design**: entities, modules, class
  responsibilities, REST APIs, and the core hold→pay→confirm flow.
- [`docs/DATABASE.md`](docs/DATABASE.md) — DB topology (schema-per-module), the cross-module
  transaction inventory, cross-module read/write/event patterns, and the future service-split map.
- [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md) — the phased build plan and resolved decisions.
- [`docs/modules/<module>.md`](docs/modules) — a living design doc per module (endpoints, layers,
  decisions, how to test).
- [`docs/design`](docs/design) — the original raw requirements/notes.

---

## Assumptions & scope

Carried over from the requirements ([`docs/design`](docs/design)):

- **Checkout is a preview step.** After selecting seats the customer lands on checkout showing the
  payable amount, payment methods, and a coupon field. Entering a coupon "reloads" the payable amount
  (`GET /holds/{id}/checkout?discountCode=`).
- **Payment is simulated** — a gateway with a configurable wait and success/failure outcome. There is
  no real redirect or debit, and the full payment lifecycle (partial captures, chargebacks, etc.) is
  not modelled. `POST /payments` returns the SUCCESS/FAILED result; success confirms the booking.
- **Cancellation is allowed only before showtime**, subject to the show's refund policy (e.g. the
  30-minute `0%` rule is the cutoff). The refund payout is simulated (instant `PROCESSED`).
- **Notifications are simulated via logs** — no real email/SMS/push is sent.

Decisions made during the build (documented per module and in `DATABASE.md`):

- **Money is stored as `BIGINT` paise** everywhere; enums are stored as `VARCHAR`.
- **Auth is a stateless JWT access token only** (no refresh/logout in v1) + role-based access control.
  The first ADMIN is seeded via a Flyway migration (`admin@moviebooking.com` / `Admin@12345` — **rotate
  after first login**).
- **Seat holds have a TTL** (default 5 min, `app.booking.hold-ttl`); a scheduled sweeper releases
  expired holds, and every read/use path also checks expiry lazily, so correctness never depends on the
  sweeper.
- **Usage of a promo code is counted at booking confirmation** and released on cancellation.
- **`show` uses a denormalized read-model** — it snapshots the movie/venue/seat display fields at show
  creation, so browse/detail/seat-map are pure show-schema queries (no cross-module calls at query
  time). Snapshots can go stale on a catalog rename (rare).
- **Cross-module boundaries:** references by id (no cross-schema FK); each module publishes a small
  `<module>.api` facade for what others need; async reactions cross modules as domain events.

**Out of scope / nice-to-have (pluggable):**

- **Auto-applied OFFER discounts** — the `Discount` model has a `PROMO/OFFER` discriminator ready, but
  only PROMO is implemented.
- **Seat layout mapping** — seats are row/number only (no visual layout).
- **Per-user communication preferences** — the notification service is multi-channel and pluggable, but
  channel selection is a fixed default (EMAIL + PUSH); `SmsChannelSender` is wired but unused by default.

---

## Prerequisites

- **JDK 17**
- **PostgreSQL 14+** running locally (the project was developed against PostgreSQL 17)
- No Docker required. A `Gradle` wrapper is included (`./gradlew`).

### One-time database setup

Create the dev and test databases and the application role (matches the default config):

```bash
psql -d postgres -c "CREATE DATABASE ticket_booking;"
psql -d postgres -c "CREATE DATABASE ticket_booking_test;"
psql -d postgres -c "CREATE ROLE ticket_booking LOGIN PASSWORD 'ticket_booking';"
psql -d postgres -c "ALTER DATABASE ticket_booking      OWNER TO ticket_booking;"
psql -d postgres -c "ALTER DATABASE ticket_booking_test OWNER TO ticket_booking;"
```

Flyway applies all migrations automatically on startup.

---

## Configuration

Datasource and app settings live in [`src/main/resources/application.yaml`](src/main/resources/application.yaml)
with sensible dev defaults, overridable by environment variables. Tests use
`src/test/resources/application.yaml` (the `ticket_booking_test` DB).

| Env var | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ticket_booking` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `ticket_booking` / `ticket_booking` | DB credentials |
| `JWT_SECRET` | dev secret (≥32 bytes) | JWT signing key — **set a real secret in prod** |
| `JWT_EXPIRATION` | `24h` | Access-token lifetime |
| `HOLD_TTL` | `5m` | Seat-hold TTL |
| `PAYMENT_DELAY` / `PAYMENT_FAILURE_RATE` | `300ms` / `0.0` | Simulated gateway delay / failure probability |
| `REMINDER_WINDOW` | `24h` | How far ahead show reminders are sent |

> The committed DB password and JWT secret are **dev defaults only**. In any real environment, supply
> secrets via env vars / a secrets manager and never commit them.

---

## Build & run

```bash
./gradlew bootRun        # starts on http://localhost:8080 (applies Flyway migrations)
./gradlew build          # compile + test + build the runnable jar
java -jar build/libs/ticket-booking-0.0.1.jar
```

Health check: `GET http://localhost:8080/actuator/health`.

---

## Testing

```bash
./gradlew test           # runs all tests; JaCoCo coverage report runs automatically after
```

- **136 tests** across three layers: **unit** (Mockito service logic), **web-slice** (standalone
  MockMvc controller/validation), and **integration** (`@SpringBootTest` against real Postgres,
  including the mandatory concurrency test).
- Requires the local `ticket_booking_test` database to be reachable.

Run a subset or the headline concurrency test:

```bash
./gradlew test --tests "com.moviebooking.ticket_booking.booking.BookingConcurrencyTest"
```

**Coverage** (JaCoCo): open the HTML report after a test run:

```bash
open build/reports/jacoco/test/html/index.html      # ~90% line / instruction coverage
```

---

## API overview

Base path `/api/v1`. Errors are RFC-7807 Problem Details (`application/problem+json`) with an
`errorCode`. Access: 🔓 public · 👤 CUSTOMER · 🛡 ADMIN.

- **Auth** 🔓 — `POST /auth/register`, `POST /auth/login`; `GET /auth/me` 👤
- **Catalog** — 🛡 `/admin/{cities,theatres,screens,movies}` CRUD, `/admin/screens/{id}/seats`;
  🔓 `GET /cities`, `GET /movies?q=`
- **Shows** — 🛡 `POST/PUT /admin/shows`, `POST /admin/shows/{id}/cancel`;
  🔓 `GET /shows?cityId=&movieId=&date=`, `GET /shows/{id}`, `GET /shows/{id}/seats`
- **Discounts** 🛡 — `/admin/discounts` CRUD
- **Refund policies** 🛡 — `/admin/refund-policies` CRUD
- **Holds** 👤 — `POST /holds`, `GET /holds/{id}`, `DELETE /holds/{id}`, `GET /holds/{id}/checkout?discountCode=`
- **Bookings** 👤 — `POST /bookings`, `GET /bookings`, `GET /bookings/{id}`, `POST /bookings/{id}/cancel`
- **Payments** 👤 — `POST /payments`, `GET /payments/{id}`
- **Refunds** 👤 — `GET /refunds?bookingId=`

Full contracts are in [`docs/DESIGN.md`](docs/DESIGN.md) §4 and each module doc.

---

## Postman collection

Import [`postman/ticket-booking.postman_collection.json`](postman/ticket-booking.postman_collection.json)
into Postman. It is organized by phase; run the folders top to bottom:

1. **Phase 1 – Auth** (log in as admin — saves `{{accessToken}}`)
2. **Phase 2 – Catalog** → **Phase 4 – Shows** → **Phase 5 – Discounts** → **Phase 3 – Refund Policies**
3. Log in as a customer, then **Phase 6 – Booking** (hold → checkout → book) → **Phase 7 – Payment** →
   cancel → **Phase 8 – Refund**

Login / hold / booking requests auto-save `{{accessToken}}`, `{{holdId}}`, and `{{bookingId}}`.

## End-to-end demo script

With the app running, run the whole customer journey (catalog → browse → hold → checkout+discount →
book → pay → cancel → refund) with narrated output:

```bash
./scripts/demo.sh
# or against another host:  BASE_URL=http://host:port ./scripts/demo.sh
```

Watch the `bootRun` console for the `[EMAIL]` / `[PUSH]` notification log lines.

---

## Project structure

```
src/main/java/com/moviebooking/ticket_booking/
  auth/ catalog/ show/ discount/ booking/ payment/ refund/ notification/   # business modules
  common/                                                                  # cross-cutting foundation
src/main/resources/
  application.yaml
  db/migration/V1..V10__*.sql                                              # Flyway (schema-per-module)
docs/            # DESIGN.md, DATABASE.md, DEVELOPMENT_PLAN.md, modules/*.md
postman/         # API collection
scripts/         # demo.sh
```

Each module: `entity/ repository/ service/ handler/ web/ dto/ mapper/` (+ `api/` for its published
cross-module facade, `event/` for domain events).
