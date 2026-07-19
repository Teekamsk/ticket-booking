# Design — Movie Ticket Booking System

> Status: v1.0 — ACCEPTED. Decisions confirmed: ShowSeat/SeatHold/ShowPricing/RefundPolicy+RefundRule added to the original entity list; Cancellation kept as separate audit entity; weekend pricing via admin-set per-show prices; single User table with role enum; money as BIGINT paise; single-table Discount (PROMO/OFFER).

---

## 1. Entities

Conventions: `id` = BIGINT auto-generated PK on every table; `created_at`/`updated_at` TIMESTAMPTZ (UTC) on every table (omitted below); all money stored as **BIGINT in paise**; enums stored as VARCHAR.

### User
Single table with a role enum instead of Admin/Customer subclasses — no divergent fields justify inheritance.

| Field | Type | Notes |
|---|---|---|
| name | VARCHAR(100) | |
| email | VARCHAR(255) | UNIQUE, login identifier |
| phone | VARCHAR(15) | |
| password_hash | VARCHAR(100) | BCrypt |
| role | ENUM | ADMIN, CUSTOMER |

### City
| Field | Type | Notes |
|---|---|---|
| name | VARCHAR(100) | UNIQUE |
| state | VARCHAR(100) | |
| is_active | BOOLEAN | soft delete |

### Theatre
| Field | Type | Notes |
|---|---|---|
| city_id | FK → City | City 1—N Theatre |
| name | VARCHAR(150) | UNIQUE(city_id, name) |
| address | VARCHAR(255) | |
| is_active | BOOLEAN | |

### Screen
| Field | Type | Notes |
|---|---|---|
| theatre_id | FK → Theatre | Theatre 1—N Screen |
| name | VARCHAR(50) | UNIQUE(theatre_id, name) |
| total_seats | INT | derived, kept for display |
| is_active | BOOLEAN | |

### Seat
Physical seat of a screen (layout mapping out of scope; row/number only).

| Field | Type | Notes |
|---|---|---|
| screen_id | FK → Screen | Screen 1—N Seat |
| row_label | VARCHAR(3) | e.g. "A" |
| seat_number | INT | UNIQUE(screen_id, row_label, seat_number) |
| seat_type | ENUM | REGULAR, PREMIUM, RECLINER |
| is_active | BOOLEAN | |

### Movie
| Field | Type | Notes |
|---|---|---|
| title | VARCHAR(200) | indexed for search |
| language | VARCHAR(50) | |
| genre | VARCHAR(50) | |
| duration_min | INT | |
| certificate | ENUM | U, UA, A |
| release_date | DATE | |
| is_active | BOOLEAN | |

### Show
| Field | Type | Notes |
|---|---|---|
| movie_id | FK → Movie | |
| screen_id | FK → Screen | overlap check in service layer |
| start_time | TIMESTAMPTZ | |
| end_time | TIMESTAMPTZ | derived from movie duration |
| status | ENUM | SCHEDULED, CANCELLED, COMPLETED |
| refund_policy_id | FK → RefundPolicy | per-show configurable policy |

### ShowPricing
Per-show price per seat type. Admin sets weekend/premium pricing here at show creation (a weekend show simply gets higher prices; no separate multiplier logic needed for v1 — pluggable later).

| Field | Type | Notes |
|---|---|---|
| show_id | FK → Show | Show 1—N ShowPricing |
| seat_type | ENUM | UNIQUE(show_id, seat_type) |
| price | BIGINT | paise |

### ShowSeat (concurrency anchor)
One row per (show × seat), generated when the show is created. This is the row we lock; its status is the single source of truth for availability.

| Field | Type | Notes |
|---|---|---|
| show_id | FK → Show | UNIQUE(show_id, seat_id) ← prevents double-allocation at DB level |
| seat_id | FK → Seat | |
| status | ENUM | AVAILABLE, HELD, BOOKED |
| hold_id | FK → SeatHold, nullable | set while HELD |
| version | BIGINT | optimistic guard (secondary) |

### SeatHold (TTL holds)
A user's temporary claim on one or more ShowSeats. Expiry is enforced lazily (`expires_at < now` ⇒ treated as expired at every read/booking) + a scheduled sweeper that flips rows back to AVAILABLE.

| Field | Type | Notes |
|---|---|---|
| user_id | FK → User | |
| show_id | FK → Show | |
| status | ENUM | ACTIVE, EXPIRED, RELEASED, CONVERTED |
| expires_at | TIMESTAMPTZ | now + 5 min (configurable) |
| total_amount | BIGINT | price snapshot at hold time |

### Booking
Order-level record; one Booking → N Tickets.

| Field | Type | Notes |
|---|---|---|
| booking_ref | VARCHAR(12) | UNIQUE, human-readable (e.g. BK7F3A9C) |
| user_id | FK → User | |
| show_id | FK → Show | |
| hold_id | FK → SeatHold | audit trail |
| status | ENUM | PENDING_PAYMENT, CONFIRMED, CANCELLED, EXPIRED |
| total_amount | BIGINT | sum of ticket prices |
| discount_id | FK → Discount, nullable | |
| discount_amount | BIGINT | |
| payable_amount | BIGINT | total − discount |
| confirmed_at / cancelled_at | TIMESTAMPTZ | nullable |

### Ticket
Per-seat line item of a booking; price frozen at hold time.

| Field | Type | Notes |
|---|---|---|
| booking_id | FK → Booking | Booking 1—N Ticket |
| show_id | FK → Show | UNIQUE(show_id, seat_id) WHERE status=ACTIVE — final DB safety net |
| seat_id | FK → Seat | |
| seat_type | ENUM | denormalized snapshot |
| price | BIGINT | snapshot |
| status | ENUM | ACTIVE, CANCELLED |

### Payment
Simulated gateway; lifecycle kept minimal per assumptions.

| Field | Type | Notes |
|---|---|---|
| booking_id | FK → Booking | |
| amount | BIGINT | |
| method | ENUM | UPI, CARD, NETBANKING |
| status | ENUM | INITIATED, SUCCESS, FAILED |
| idempotency_key | VARCHAR(64) | UNIQUE — safe retries |
| txn_ref | VARCHAR(64) | simulated gateway ref |

### Discount
Single table, discriminator `type` — PROMO (code-entered, in scope) vs OFFER (auto-applied, out of scope but pluggable).

| Field | Type | Notes |
|---|---|---|
| type | ENUM | PROMO, OFFER |
| code | VARCHAR(30) | UNIQUE, null for OFFER |
| discount_type | ENUM | PERCENT, FLAT |
| value | BIGINT | percent (0–100) or paise |
| max_discount_amount | BIGINT | cap for PERCENT |
| min_order_amount | BIGINT | eligibility |
| valid_from / valid_to | TIMESTAMPTZ | |
| max_total_uses / max_uses_per_user | INT | usage tracked via DiscountRedemption(discount_id, user_id, booking_id) |
| is_active | BOOLEAN | |

### RefundPolicy / RefundRule
Makes "configurable refund policies" a data problem, not a code problem.

RefundPolicy: name VARCHAR UNIQUE, is_active BOOLEAN.
RefundRule: policy_id FK, min_minutes_before_show INT, refund_percent INT — e.g. (1440, 100), (120, 50), (30, 0). Rule matched by largest `min_minutes_before_show` ≤ gap. Cancellation cutoff (30 min) = rule with 0%.

### Cancellation
| Field | Type | Notes |
|---|---|---|
| booking_id | FK → Booking | UNIQUE (one cancellation per booking) |
| cancelled_by | FK → User | |
| reason | VARCHAR(255) | optional |
| refund_percent_applied | INT | snapshot of matched rule |


### Refund
| Field | Type | Notes |
|---|---|---|
| cancellation_id | FK → Cancellation | |
| payment_id | FK → Payment | refund against original payment |
| amount | BIGINT | |
| status | ENUM | INITIATED, PROCESSED, FAILED |
| processed_at | TIMESTAMPTZ | simulated |

### Notification
Persisted record; delivery simulated via logging, processed async.

| Field | Type | Notes |
|---|---|---|
| user_id | FK → User | |
| booking_id | FK → Booking, nullable | |
| type | ENUM | BOOKING_CONFIRMED, BOOKING_CANCELLED, REFUND_PROCESSED, SHOW_REMINDER |
| channel | ENUM | EMAIL, SMS (simulated) |
| payload | TEXT | rendered message |
| status | ENUM | PENDING, SENT, FAILED |
| sent_at | TIMESTAMPTZ | |

### Relationship summary
City 1—N Theatre 1—N Screen 1—N Seat • Movie 1—N Show N—1 Screen • Show 1—N ShowPricing, 1—N ShowSeat • User 1—N SeatHold 1—N ShowSeat(held) • User 1—N Booking 1—N Ticket • Booking 1—1 Payment, 1—0..1 Cancellation 1—0..1 Refund • RefundPolicy 1—N RefundRule, 1—N Show • Discount N—N User (via DiscountRedemption) • User 1—N Notification

---

## 2. Architectural Business Modules

Package-by-module (modular monolith): `com.teekam.moviebooking.<module>`, each module owning its controller/service/repository/entity/dto internally. Cross-module calls go service→service, never repo-of-another-module.

| Module | Owns | Responsibility |
|---|---|---|
| `auth` | User | registration, login (JWT), role checks |
| `catalog` | City, Theatre, Screen, Seat, Movie | admin CRUD + customer browse/search |
| `show` | Show, ShowPricing, ShowSeat | show scheduling, pricing setup, seat-map/availability |
| `booking` | SeatHold, Booking, Ticket, Cancellation | holds w/ TTL, checkout, confirmation, cancellation — **all locking lives here** |
| `payment` | Payment | simulated payment with delay, idempotency |
| `discount` | Discount, DiscountRedemption | admin CRUD, validation + computation (pluggable strategy) |
| `refund` | RefundPolicy, RefundRule, Refund | policy CRUD, rule evaluation, refund processing |
| `notification` | Notification | async listeners (AFTER_COMMIT), simulated senders, reminder scheduler |
| `common` | — | error handling (RFC-7807), security config, base entity, money utils, scheduler config |

---

## 3. Classes per Module

Naming: `XController` → `XService` (interface optional; concrete class fine for this scope) → `XRepository extends JpaRepository`.

**auth:** `AuthController` • `AuthService`, `JwtService` • `UserRepository` • `User`

**catalog:** `AdminCatalogController` (admin CRUD), `CatalogController` (public browse) • `CityService`, `TheatreService`, `ScreenService`, `MovieService` • `CityRepository`, `TheatreRepository`, `ScreenRepository`, `SeatRepository`, `MovieRepository` • `City`, `Theatre`, `Screen`, `Seat`, `Movie`

**show:** `AdminShowController`, `ShowController` (public) • `ShowService` (create show → generates ShowSeat rows + validates overlap), `ShowSearchService`, `SeatAvailabilityService` • `ShowRepository`, `ShowPricingRepository`, `ShowSeatRepository` (holds the `@Lock(PESSIMISTIC_WRITE)` / `FOR UPDATE` queries) • `Show`, `ShowPricing`, `ShowSeat`

**booking:** `HoldController`, `BookingController` • `SeatHoldService` (create/release holds, price snapshot), `BookingService` (checkout, confirm-on-payment-success), `CancellationService`, `HoldExpirySweeper` (@Scheduled) • `SeatHoldRepository`, `BookingRepository`, `TicketRepository`, `CancellationRepository` • `SeatHold`, `Booking`, `Ticket`, `Cancellation`

**payment:** `PaymentController` • `PaymentService`, `PaymentGateway` (interface) + `SimulatedPaymentGateway` (configurable delay + failure rate) • `PaymentRepository` • `Payment`

**discount:** `AdminDiscountController` • `DiscountService` (validate + compute; `DiscountStrategy` interface with `PromoCodeStrategy`, OFFER strategy pluggable later) • `DiscountRepository`, `DiscountRedemptionRepository` • `Discount`, `DiscountRedemption`

**refund:** `AdminRefundPolicyController` • `RefundPolicyService`, `RefundService` (rule evaluation + simulated processing) • `RefundPolicyRepository`, `RefundRuleRepository`, `RefundRepository` • `RefundPolicy`, `RefundRule`, `Refund`

**notification:** *(no controller — internal)* • `NotificationService`, `BookingEventListener` (`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`), `NotificationSender` (interface) + `LoggingNotificationSender`, `ReminderScheduler` (@Scheduled) • `NotificationRepository` • `Notification`

**common:** `GlobalExceptionHandler` (@RestControllerAdvice, Problem Details), `SecurityConfig`, `AsyncConfig`, `BaseEntity`, domain exceptions (`SeatUnavailableException`, `HoldExpiredException`, …)

---

## 4. REST APIs

Base path `/api/v1`. 🔓 public • 👤 CUSTOMER • 🛡 ADMIN. Errors: RFC-7807; 409 for seat conflicts; 410 for expired holds.

### AuthController
| Method | Path | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | 🔓 | register customer |
| POST | `/auth/login` | 🔓 | returns JWT |

### AdminCatalogController — `/admin/...` 🛡
| Method | Path | Purpose |
|---|---|---|
| POST/PUT/DELETE | `/admin/cities`, `/admin/cities/{id}` | manage cities (DELETE = deactivate) |
| POST/PUT/DELETE | `/admin/theatres`, `/admin/theatres/{id}` | manage theatres |
| POST/PUT | `/admin/screens`, `/admin/screens/{id}` | manage screens |
| POST | `/admin/screens/{id}/seats` | bulk seat creation (rows × numbers × type) |
| POST/PUT/DELETE | `/admin/movies`, `/admin/movies/{id}` | manage movies |

### CatalogController 🔓
| Method | Path | Purpose |
|---|---|---|
| GET | `/cities` | list active cities |
| GET | `/movies?cityId=&q=` | movies running in a city, title search |

### AdminShowController 🛡
| Method | Path | Purpose |
|---|---|---|
| POST | `/admin/shows` | create show + pricing per seat_type + refund policy (generates ShowSeats) |
| PUT | `/admin/shows/{id}` | update (restricted once bookings exist) |
| POST | `/admin/shows/{id}/cancel` | cancel show |

### ShowController 🔓
| Method | Path | Purpose |
|---|---|---|
| GET | `/shows?cityId=&movieId=&date=` | search shows (grouped by theatre) |
| GET | `/shows/{id}` | show details + pricing |
| GET | `/shows/{id}/seats` | seat map with live availability (AVAILABLE/HELD/BOOKED) |

### HoldController 👤
| Method | Path | Purpose |
|---|---|---|
| POST | `/holds` | `{showId, seatIds[]}` → hold with expires_at; 409 if any seat taken |
| GET | `/holds/{id}` | hold status + countdown |
| DELETE | `/holds/{id}` | release manually |
| GET | `/holds/{id}/checkout?discountCode=` | checkout preview: amount breakdown, re-callable with code (matches "reload with new payable amount") |

### BookingController 👤
| Method | Path | Purpose |
|---|---|---|
| POST | `/bookings` | `{holdId, discountCode?}` → PENDING_PAYMENT booking; freezes amounts |
| GET | `/bookings` | booking history (paged) |
| GET | `/bookings/{id}` | booking detail + tickets |
| POST | `/bookings/{id}/cancel` | cancel (≥30 min before show per policy) → triggers refund |

### PaymentController 👤
| Method | Path | Purpose |
|---|---|---|
| POST | `/payments` | `{bookingId, method, idempotencyKey}` → simulated pay; SUCCESS ⇒ booking CONFIRMED, seats BOOKED, notification event |
| GET | `/payments/{id}` | payment status |

### AdminDiscountController 🛡
| Method | Path | Purpose |
|---|---|---|
| POST/PUT/DELETE | `/admin/discounts`, `/admin/discounts/{id}` | manage promo codes |
| GET | `/admin/discounts` | list |

### AdminRefundPolicyController 🛡
| Method | Path | Purpose |
|---|---|---|
| POST/PUT | `/admin/refund-policies`, `/{id}` | policy + rules |
| GET | `/admin/refund-policies` | list |

---

## 5. Core flow (hold → pay → confirm)

1. `POST /holds`: TX — lock ShowSeat rows (`FOR UPDATE`, ordered by id to avoid deadlock) → verify all AVAILABLE (or expired-HELD) → set HELD + hold_id, create SeatHold(expires_at = now+5m, price snapshot). Any conflict ⇒ 409, nothing held.
2. `GET /holds/{id}/checkout`: validate hold not expired, compute discount, return breakdown.
3. `POST /bookings`: validate ACTIVE non-expired hold → Booking PENDING_PAYMENT, freeze amounts.
4. `POST /payments`: simulate delay → on SUCCESS, TX: booking CONFIRMED, tickets created, ShowSeats → BOOKED, hold CONVERTED; publish event → async notification AFTER_COMMIT. On FAILED: booking stays PENDING_PAYMENT until hold expiry releases seats.
5. Sweeper (@Scheduled, 30s): expired ACTIVE holds → EXPIRED, their ShowSeats → AVAILABLE. Lazy expiry check everywhere means correctness never depends on the sweeper.