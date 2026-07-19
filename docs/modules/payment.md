# Module: `payment` (Phase 7)

Owns `Payment`. A **simulated** gateway (configurable delay + failure rate) — no real redirect or
debit. On success it confirms the booking via the `booking.api` hook from Phase 6. Schema: `payment`.

## Endpoints — `/api/v1/payments` 👤 CUSTOMER (`PaymentController`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/` | `{bookingId, method, idempotencyKey}` → simulated pay; SUCCESS confirms the booking |
| GET | `/{id}` | Payment status (owner-scoped) |

`POST /payments` returns `200` with the payment result (SUCCESS or FAILED) in the body — a FAILED
attempt is still a recorded resource, not an HTTP error.

## Flow

1. **Idempotency replay:** if a payment already exists for `idempotencyKey`, return it (no re-charge).
2. **`bookingConfirmationService.loadPayable(bookingId, userId)`** — validates the booking is the
   caller's and `PENDING_PAYMENT`, returns the payable amount (`422` if not awaiting payment).
3. **Gateway charge** — `SimulatedPaymentGateway` waits `app.payment.delay`, then succeeds unless a
   random draw is below `app.payment.failure-rate`. Runs **outside any transaction**.
4. **Finalize (one transaction, `PaymentProcessor`):** persist the Payment; on SUCCESS call
   `bookingConfirmationService.confirm(bookingId)` (tickets, seats→BOOKED, hold→CONVERTED, discount
   recorded, `BookingConfirmedEvent` published). On FAILED, the booking stays `PENDING_PAYMENT` and
   the hold keeps the seats until it expires.

The unique `idempotency_key` is the concurrency safety net: simultaneous same-key requests → one
persists, the other gets `409 DATA_CONFLICT`.

## Layers (`com.moviebooking.ticket_booking.payment`)

- **entity** — `Payment` (`booking_id`/`user_id` cross-module ids, no FK; `idempotency_key` unique).
  Enums `PaymentMethod` (UPI/CARD/NETBANKING), `PaymentStatus` (INITIATED/SUCCESS/FAILED).
- **gateway** — `PaymentGateway` interface + `SimulatedPaymentGateway` + `GatewayOutcome`.
- **service** — `PaymentService` (orchestration: idempotency, load, charge outside tx),
  `PaymentProcessor` (`@Transactional` persist + confirm — split out so the delay stays out of the tx).
- **dto / mapper / handler / web** — `CreatePaymentRequest`/`PaymentResponse`, `PaymentMapper`,
  `PaymentRequestHandler`, `PaymentController`.

## Key decisions

- **Gateway delay outside the transaction** — a dedicated `PaymentProcessor` bean holds the
  `@Transactional` persist+confirm so the simulated wait never holds a DB connection open.
  (Also avoids the self-invocation trap where `@Transactional` on a same-class method is ignored.)
- **Idempotency by client key** — replay returns the same payment; concurrent same-key relies on the
  unique constraint.
- **Failure rate defaults to 0.0** (deterministic) in dev and tests; delay defaults to `300ms` in dev,
  `0ms` in tests. Force the failure path in a test with `app.payment.failure-rate=1.0`.
- Payment reads/needs booking data only through `booking.api` (`loadPayable` + `confirm`) — never
  booking's repositories.

## How to test

- **Automated:** `./gradlew test` — `PaymentProcessorTest` (success confirms / failure doesn't),
  `PaymentServiceTest` (idempotent replay vs. charge+finalize), `SimulatedPaymentGatewayTest`
  (rate 0 vs 1), `PaymentControllerTest` (validation + invalid enum),
  `PaymentFlowIntegrationTest` (pay → CONFIRMED + seats BOOKED, idempotent retry, pay-already-confirmed
  `422`, no-token `401`), `PaymentFailureTest` (forced FAILED → booking stays PENDING, seats HELD).
- **Manual:** `./gradlew bootRun`, Postman folder *Phase 7 - Payment* (create a PENDING booking first;
  set `HOLD`/`PAYMENT_FAILURE_RATE` env to explore paths).

## Migration

`V8__create_payments.sql` — `payment` schema; `payment.payments` (`uq_payment_idempotency`,
index on `booking_id`). See [`../DATABASE.md`](../DATABASE.md).
