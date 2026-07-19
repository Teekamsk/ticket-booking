# Module: `show` (Phase 4)

Owns `Show`, `ShowPricing`, and `ShowSeat` — the **first module that references other modules by
id** (catalog movie/screen, refund policy). It generates the `ShowSeat` rows that are the
concurrency anchor booking will lock in Phase 6. Schema: `show`.

## Cross-module boundary (key design)

`show` never joins or FKs into `catalog`/`refund`. Instead:

- **Validation + snapshot at creation** via `catalog.api.CatalogQueryService` (a published read
  facade returning `MovieSummary` / `ScreenLocation` / `SeatView` records — not catalog entities)
  and `RefundPolicyService.requireActive(id)`.
- **Full denormalized read-model:** the `Show` row stores the catalog snapshot (movie id + title/
  language/certificate/duration, screen/theatre/city ids + names); `ShowSeat` stores the seat
  snapshot (seat id, row, number, type). **All show reads (search/detail/seat-map) are pure
  show-schema queries — zero catalog calls at query time.** Trade-off: names go stale if catalog
  renames later (rare; a refresh job can be added). Chosen for clean future service extraction.

## Endpoints

### Admin — `/api/v1/admin/shows` 🛡 ADMIN (`AdminShowController`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/` | Create show (validates + snapshots catalog, derives end_time, generates ShowSeats, sets pricing) (201) |
| PUT | `/{id}` | Update **start_time and/or pricing**, only while all seats AVAILABLE |
| POST | `/{id}/cancel` | Cancel a scheduled show |

### Public — `/api/v1/shows` 🔓 (`ShowController`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/?cityId=&movieId=&date=` | Search (cityId required), grouped by theatre |
| GET | `/{id}` | Show detail + pricing |
| GET | `/{id}/seats` | Live seat map (AVAILABLE/HELD/BOOKED) with per-seat price |

## Layers (`com.moviebooking.ticket_booking.show`)

- **entity** — `Show` (denormalized snapshot + `@OneToMany` pricing), `ShowPricing`, `ShowSeat`
  (`@Version` optimistic guard; `hold_id` plain id for Phase 6). Enums `ShowStatus`
  (SCHEDULED/CANCELLED/COMPLETED), `ShowSeatStatus` (AVAILABLE/HELD/BOOKED). `seat_type` stored as
  String (decoupled from catalog's enum).
- **repository** — `ShowRepository` (`overlaps` guard, denormalized `search`, `@EntityGraph`
  pricing), `ShowSeatRepository`.
- **service** — `ShowService` (create/update/cancel + `getOrThrow`), `ShowSearchService`,
  `SeatAvailabilityService`. Web-agnostic commands `PriceItem`, holder `SeatMap`.
- **dto / mapper / handler / web** — request/response records with validation, `ShowMapper`
  (incl. group-by-theatre), `AdminShowRequestHandler` + `ShowQueryHandler`, the two controllers.

## Key behaviours & decisions

- **end_time = start_time + movie.durationMin** (snapshotted duration).
- **Overlap guard:** no two SCHEDULED shows on the same screen may overlap `[start, end)` → `409`.
- **Pricing coverage:** the price set must cover *exactly* the distinct seat types on the screen
  (missing/extra/duplicate → `422`).
- **start_time must be in the future** (`@Future` + service check).
- **PUT is limited** to start_time (overlap re-checked) and pricing, and only while every ShowSeat
  is AVAILABLE (`422` otherwise). Movie/screen changes are not allowed. Guard tightens automatically
  once holds/bookings exist (Phase 6).
- **Cancel** sets CANCELLED; blocked if any seat is BOOKED (refund-driven cancellation is Phase 8).
- **Search is city-scoped** (cityId required); movieId + date optional; default window = next 90 days.

## New error codes (in addition to earlier phases)

| Status | errorCode | When |
|---|---|---|
| 400 | `MISSING_PARAMETER` | Required query param absent (e.g. `cityId`). |
| 400 | `INVALID_PARAMETER` | Query param wrong type (e.g. bad `date`/`id`). |

## How to test

- **Automated:** `./gradlew test` — `ShowServiceTest` (create snapshot + seat generation, overlap,
  pricing coverage, past start, update-guard, cancel; catalog/refund mocked), `AdminShowControllerTest`
  (validation/status), `ShowIntegrationTest` (create→detail→seat-map→search→update→cancel, overlap
  `409`, coverage `422`, past-start `400`, RBAC 401/403, missing `cityId` `400`).
- **Manual:** `./gradlew bootRun`, Postman folders *Phase 4 - Shows (Admin)* / *(Public)* (create the
  catalog + a refund policy first).

## Migration

`V5__create_shows.sql` — `show` schema; `show.shows` (denormalized, search indexes on
`(city_id, movie_id, start_time)` and `(screen_id, start_time)`), `show.show_pricing`
(`uq_show_pricing_type`), `show.show_seats` (`uq_show_seat`, `version`). See [`../DATABASE.md`](../DATABASE.md).
