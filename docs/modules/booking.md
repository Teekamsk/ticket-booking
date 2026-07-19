# Module: `booking` (Phase 6)

The heart of the system, and the concurrency-critical one. Owns `SeatHold`, `Booking`, `Ticket`,
`Cancellation`. Orchestrates the hold → checkout → book → confirm → cancel lifecycle. Schema: `booking`.

## The double-booking guarantee

Serializing concurrent bookings is the headline requirement. It is enforced in **two layers**:

1. **Pessimistic lock at hold time.** `SELECT … FOR UPDATE` on `show.show_seats` (owned by the
   `show` module, exposed via `show.api.SeatReservationService.lockSeats`, ordered by id to avoid
   deadlocks). Booking calls this inside its own `@Transactional`, checks each seat is AVAILABLE (or
   reclaimable from an expired hold), then flips them to HELD — all in one transaction. Any conflict
   rolls back (nothing held) → `409`.
2. **Partial unique index as the final safety net.** `uq_ticket_show_seat_active` =
   `UNIQUE (show_id, seat_id) WHERE status = 'ACTIVE'`. If two confirmations ever raced to the same
   seat, the second ticket insert violates it → `409 DATA_CONFLICT`.

Verified by `BookingConcurrencyTest`: 8 threads racing for one seat → exactly one `201`, seven `409`.

## Cross-module ownership (key architecture)

Booking never writes another schema directly. ShowSeat lock + transitions live in
`show.api.SeatReservationService` (show owns the table); booking calls it within its transaction —
same DB, one transaction, so ACID + FOR UPDATE hold. This refines DESIGN's "locking lives in
booking" to respect the schema-per-module boundary. Other facades used: `discount.api`
(validate/record/release), `refund.api` (policy evaluation), `show.api` (show snapshot).

## Endpoints (👤 CUSTOMER)

### Holds — `/api/v1/holds` (`HoldController`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/` | `{showId, seatIds[]}` → hold (TTL); `409` if any seat taken |
| GET | `/{id}` | Hold status + countdown |
| DELETE | `/{id}` | Release manually |
| GET | `/{id}/checkout?discountCode=` | Amount breakdown (re-callable with a code) |

### Bookings — `/api/v1/bookings` (`BookingController`)
| Method | Path | Purpose |
|---|---|---|
| POST | `/` | `{holdId, discountCode?}` → PENDING_PAYMENT booking; amounts frozen |
| GET | `/?page=&size=` | Booking history (paged, owner-scoped) |
| GET | `/{id}` | Booking detail + seats/tickets |
| POST | `/{id}/cancel` | Cancel a CONFIRMED booking (policy gap check) |

## Lifecycle & services

- **`SeatHoldService`** — createHold (lock/reclaim/hold), getHold (non-mutating, effective status),
  releaseHold, checkout (applies discount), `requireActiveHold` (410 if expired/not-active).
- **`BookingService`** — createBooking (freeze total/discount/payable, snapshot show fields for
  self-contained history), getBooking, listBookings.
- **`BookingConfirmationService`** (`booking.api`) — `confirm(bookingId)`, called by **payment
  (Phase 7)** on success: create tickets, ShowSeats→BOOKED, hold→CONVERTED, record discount usage,
  publish `BookingConfirmedEvent`. Idempotent.
- **`CancellationService`** — cancel a CONFIRMED booking: gap check via `refund.api`, tickets→CANCELLED,
  ShowSeats→AVAILABLE, `Cancellation` with refund% snapshot, release discount, publish
  `BookingCancelledEvent`. **The refund payout is Phase 8** (it listens to that event).
- **`HoldExpirySweeper`** — `@Scheduled` (30s): ACTIVE holds past `expires_at` → EXPIRED + seats
  released. Correctness never depends on it — every use path checks expiry lazily.

Hold TTL is `app.booking.hold-ttl` (default 5m).

## New error codes

| Status | errorCode | When |
|---|---|---|
| 409 | `SEAT_UNAVAILABLE` | A requested seat is held/booked at hold time. |
| 410 | `HOLD_EXPIRED` | Using a hold that has expired or is no longer active. |

## How to test

- **Automated:** `./gradlew test` —
  **`BookingConcurrencyTest`** (mandatory: 8-thread race, exactly one succeeds),
  `BookingFlowIntegrationTest` (hold→checkout→book→confirm→cancel, discount checkout, re-book conflict,
  release, RBAC 401/403, paged history),
  `HoldExpiryTest` (1ms TTL → checkout/book `410`),
  `SeatHoldServiceTest` (available / booked / active-held / expired-reclaim),
  `HoldControllerTest` (validation).
- **Manual:** `./gradlew bootRun`, Postman folder *Phase 6 - Booking* (log in as a customer; create a
  show first). Confirmation has no endpoint yet — it arrives with payment in Phase 7.

## Migration

`V7__create_bookings.sql` — `booking` schema; `seat_holds`, `bookings` (denormalized show snapshot,
`uq_booking_ref`), `tickets` (+ the partial unique safety-net index), `cancellations`
(`uq_cancellation_booking`). See [`../DATABASE.md`](../DATABASE.md).
