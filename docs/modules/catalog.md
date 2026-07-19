# Module: `catalog` (Phase 2)

Owns the venue hierarchy and film catalog: **City → Theatre → Screen → Seat**, plus **Movie**.
Admin CRUD (RBAC) + public browse. Shows/pricing come in Phase 4.

## Endpoints

### Admin — `/api/v1/admin/**` 🛡 ADMIN (`AdminCatalogController`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/cities` | Create city (201) |
| PUT | `/cities/{id}` | Update city |
| DELETE | `/cities/{id}` | Deactivate city (204, soft delete) |
| POST | `/theatres` | Create theatre (needs active city) |
| PUT | `/theatres/{id}` | Update theatre |
| DELETE | `/theatres/{id}` | Deactivate theatre (204) |
| POST | `/screens` | Create screen (needs active theatre) |
| PUT | `/screens/{id}` | Update screen |
| POST | `/screens/{id}/seats` | Bulk-create seats by row block (201) |
| GET | `/screens/{id}/seats` | List a screen's seats |
| POST | `/movies` | Create movie (201) |
| PUT | `/movies/{id}` | Update movie |
| DELETE | `/movies/{id}` | Deactivate movie (204) |

### Public — `/api/v1` 🔓 (`CatalogController`)
| Method | Path | Purpose |
|---|---|---|
| GET | `/cities` | List active cities (name-sorted) |
| GET | `/movies?q=` | Active movies, optional title search |

## Layers (`com.moviebooking.ticket_booking.catalog`)

- **entity** — `City`, `Theatre` (→City), `Screen` (→Theatre), `Seat` (→Screen), `Movie`;
  enums `SeatType` (REGULAR/PREMIUM/RECLINER), `Certificate` (U/UA/A). `@ManyToOne` parent links
  are used within the module; soft delete via `is_active`.
- **repository** — one per entity; uniqueness + active-filter queries.
- **service** — `CityService`, `TheatreService`, `ScreenService` (owns seat generation),
  `MovieService`. Cross-entity parent lookups go through the parent service's `getActiveOrThrow`.
  `SeatRowSpec` is the service-level command (keeps services web-DTO agnostic).
- **dto** — per-request request/response records; validation with messages. No entity leaves the API.
- **mapper** — `CatalogMapper` (entity → response), used by handlers.
- **handler** — `CityRequestHandler`, `TheatreRequestHandler`, `ScreenRequestHandler`,
  `MovieRequestHandler`, `CatalogBrowseHandler` (public). Orchestrate + map.
- **web** — `AdminCatalogController` (class-level `@PreAuthorize("hasRole('ADMIN')")`),
  `CatalogController` (public).

## Key behaviours & decisions

- **Bulk seats = row blocks:** body is `{"rows":[{"rowLabel","seatType","count"}]}`; each block
  generates seats `1..count`. Row labels are upper-cased; duplicate rows in one request → `422`,
  a row that already exists on the screen → `409`. Atomic: any conflict creates nothing.
  `Screen.total_seats` is incremented on success.
- **Soft delete, no cascade:** deactivating a City/Theatre only flips its own `is_active`.
  Children remain in the DB but are unreachable in public browse via the inactive parent.
  Reactivating the parent restores visibility. (Screen/Movie also soft-delete; screen has no
  DELETE endpoint per DESIGN.)
- **`GET /movies?cityId=` deferred to Phase 4** — "movies running in a city" needs `Show`.
  Phase 2 ships title search (`q`) only; no dead parameter.
- Parent must be **active** to add a child (`getActiveOrThrow` → `422` if inactive, `404` if missing).
- **Added beyond DESIGN:** `GET /admin/screens/{id}/seats` (admin seat listing) for convenience.

## New error codes (in addition to Phase 0/1)

| Status | errorCode | When |
|---|---|---|
| 400 | `MALFORMED_REQUEST` | Unreadable JSON or invalid enum (e.g. bad `certificate`/`seatType`). |
| 404 | `RESOURCE_NOT_FOUND` | Missing city/theatre/screen/movie (or unmapped path). |
| 422 | `BUSINESS_RULE_VIOLATION` | Inactive parent, or duplicate row within a seat request. |

## How to test

- **Automated:** `./gradlew test` — `CityServiceTest`, `ScreenServiceTest` (seat generation +
  duplicate/existing-row conflicts), `MovieServiceTest` (browse branch), `AdminCatalogControllerTest`
  (validation/status, standalone), `CatalogIntegrationTest` (full admin chain → public browse, RBAC
  401/403, validation `400`, invalid enum `400`, missing parent `404`, soft-delete hiding).
- **Manual:** `./gradlew bootRun`, Postman folders *Phase 2 - Catalog (Admin)* / *(Public)*
  (log in as admin first to populate `{{accessToken}}`).

## Migration

`V3__create_catalog.sql` — creates the `catalog` schema and `catalog.cities/theatres/screens/seats/movies`
with the uniqueness constraints from DESIGN (`uq_city_name`, `uq_theatre_city_name`,
`uq_screen_theatre_name`, `uq_seat_screen_row_number`) and search indexes on `seats(screen_id)` and
`movies(title)`. FKs are intra-schema only (Theatre→City→Screen→Seat). See [`../DATABASE.md`](../DATABASE.md).
