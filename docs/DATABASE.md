# Database Topology

**One physical PostgreSQL database, one schema per module.** Chosen to keep the booking core's
cross-module ACID transactions and pessimistic row locking intact, while drawing boundaries clean
enough to extract modules into separate services later.

## Layout

| Schema | Owner module | Tables |
|---|---|---|
| `auth` | auth | `users` |
| `catalog` | catalog | `cities`, `theatres`, `screens`, `seats`, `movies` |
| `show` | show (Phase 4) | `shows`, `show_pricing`, `show_seats` |
| `booking` | booking (Phase 6) | `seat_holds`, `bookings`, `tickets`, `cancellations` |
| `payment` | payment (Phase 7) | `payments` |
| `discount` | discount (Phase 5) | `discounts`, `discount_redemptions` |
| `refund` | refund (Phase 3/8) | `refund_policies`, `refund_rules`, `refunds` |
| `notification` | notification (Phase 9) | `notifications` |
| `public` | — | `flyway_schema_history` |

## Rules

1. **Every entity declares its schema:** `@Table(name = "…", schema = "<module>")`.
2. **Each module's first migration** does `CREATE SCHEMA IF NOT EXISTS <module>;` then its tables.
3. **FKs only within a module's own schema.** Across modules, use a plain indexed `BIGINT <other>_id`
   column — no cross-schema FK, no JPA `@ManyToOne` to another module's entity. Validate existence in
   the service by calling the owning module's service (e.g. `catalogService.getMovieOrThrow(id)`).
4. Flyway history lives in `public`; migrations fully-qualify table names.

### Cross-module reads (the established pattern, from Phase 4)

When a module needs another module's data:

- The owning module publishes a small read facade in a `<module>.api` package that returns **view
  records** (e.g. `catalog.api.CatalogQueryService` → `MovieSummary`/`ScreenLocation`/`SeatView`).
  Consumers depend on these records, **never** on the owner's entities or repositories.
- For data shown on the consumer's own reads, **denormalize a snapshot** onto the consumer's rows at
  write time (e.g. `show.shows` stores movie/theatre/city names; `show.show_seats` stores row/number/
  type). The consumer's queries then stay entirely within its own schema — no cross-module calls at
  query time, which is exactly what a future service split needs. Snapshots can go stale on rename;
  add a refresh path only if/when that matters.

## Why not physically separate databases per module?

Because the system's headline requirement — *serialize concurrent bookings with no double-allocation*
— is enforced with a pessimistic `SELECT … FOR UPDATE` lock on `show.show_seats` inside a transaction
that also writes `booking.seat_holds` / `booking.bookings`. That guarantee needs one connection and one
transaction. Splitting those tables across databases would force distributed transactions (2PC) or
sagas with compensation and eventual consistency — much more complexity, and it weakens the exact
property the system exists to provide.

### Cross-module transaction inventory

| Operation | Writes across modules | Needs one ACID tx? |
|---|---|---|
| `POST /holds` | booking (SeatHold) + show (ShowSeat→HELD) | **Yes** — `FOR UPDATE` on ShowSeat |
| `POST /payments` (success) | payment + booking (Booking/Ticket) + show (ShowSeat→BOOKED) | **Yes** |
| `POST /bookings` | booking + discount (redemption) | **Yes** |
| Cancellation | booking + show (ShowSeat→AVAILABLE) + refund + payment (read) | **Yes** |
| Notifications | notification only (`AFTER_COMMIT` + `@Async`) | **No** — eventual by design |

## Realistic future service map

Schema-per-module + ID boundaries make extraction feasible where it actually makes sense — which is
**not** one service per module, because the transactional core cannot be cut apart:

- **auth** — separable (booking only needs `user_id` from the JWT; no shared transaction).
- **catalog** — separable (show references movie/screen by id, at setup time, not in the booking tx).
- **show + booking + payment + discount + refund** — one transactional core, one database.
- **notification** — separable (async by design).

So ~4 deployable units, not 9. The ID-reference rule (rule 3 above) is what keeps that path open.
