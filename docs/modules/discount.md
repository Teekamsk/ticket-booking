# Module: `discount` (Phase 5)

Owns `Discount` and `DiscountRedemption`. Phase 5 delivers admin CRUD for **PROMO** codes plus the
validate-and-compute logic booking consumes at checkout. **OFFER** (auto-applied) is out of scope;
the single-table `type` discriminator is ready for it. Schema: `discount`.

## Endpoints — `/api/v1/admin/discounts` 🛡 ADMIN (`AdminDiscountController`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/` | Create a PROMO discount (201) |
| PUT | `/{id}` | Update |
| DELETE | `/{id}` | Deactivate (204, soft) |
| GET | `/` | List all |
| GET | `/{id}` | Get one |

There is **no public "apply discount" endpoint** — application happens inside the booking checkout
(Phase 6) via the cross-module facade below.

## Cross-module facade (`discount.api`)

`DiscountApplicationService` is the published surface the booking module calls:

- `validateAndCompute(code, userId, orderAmount)` → `DiscountResult(discountId, code, discountAmount)`;
  throws `DiscountNotApplicableException` (422) if invalid/expired/ineligible/used-up.
- `recordRedemption(discountId, userId, bookingId, amount)` — idempotent per (discount, booking);
  **called at booking confirmation** (usage is counted then, not at checkout).
- `releaseRedemption(bookingId)` — returns the use on cancellation.

## Eligibility checks (validateAndCompute)

PROMO + active · within `[validFrom, validTo]` · `orderAmount ≥ minOrderAmount` ·
total redemptions `< maxTotalUses` (if set) · per-user redemptions `< maxUsesPerUser` (if set).
Any failure → `422 DISCOUNT_NOT_APPLICABLE` with a specific message.

## Computation (`DiscountCalculator`)

- **PERCENT:** `orderAmount * value / 100`, capped at `maxDiscountAmount` if set.
- **FLAT:** `value` paise.
- Result is always clamped to `≤ orderAmount`.

## Layers (`com.moviebooking.ticket_booking.discount`)

- **entity** — `Discount` (single table; `type` PROMO/OFFER, `discount_type` PERCENT/FLAT, validity
  window, usage caps), `DiscountRedemption` (`discount` FK intra-schema; `user_id`/`booking_id`
  cross-module ids, no FK). Enums `DiscountType`, `DiscountValueType`.
- **repository** — `DiscountRepository`, `DiscountRedemptionRepository` (count/exists/delete-by-booking).
- **service** — `DiscountService` (admin CRUD + validation), `DiscountCalculator` (pure),
  `DiscountCommand` (web-agnostic). **api** — `DiscountApplicationService` + `DiscountResult`.
- **dto / mapper / handler / web** — `DiscountRequest`/`DiscountResponse`, `DiscountMapper`,
  `DiscountRequestHandler`, `AdminDiscountController`.

## Key decisions

- **Concise strategy** (chosen): a `DiscountCalculator` + `DiscountService`, extensibility via the
  `type` enum — no speculative single-implementation `DiscountStrategy` interface (guideline 2/3).
  An OFFER path plugs in later without disturbing PROMO.
- **Usage counted at confirmation, released on cancel** — abandoned/PENDING bookings don't burn a
  use. This is why the facade exposes record + release rather than a reserve-at-checkout call.
- Admin creates **PROMO only** (`type` forced to PROMO); code unique; PERCENT value 1–100, FLAT > 0;
  `validTo` must be after `validFrom` (service-level, `422`).

## New error code

| Status | errorCode | When |
|---|---|---|
| 422 | `DISCOUNT_NOT_APPLICABLE` | Code invalid/expired/ineligible/used-up at apply time. |

## How to test

- **Automated:** `./gradlew test` — `DiscountCalculatorTest` (PERCENT cap/uncapped, FLAT, clamp),
  `DiscountServiceTest` (create/duplicate/range/validity), `DiscountApplicationServiceTest`
  (eligibility failures + release), `AdminDiscountControllerTest` (validation/status),
  `DiscountIntegrationTest` (admin CRUD + RBAC, and the **validate→record→release cycle** against the DB).
- **Manual:** `./gradlew bootRun`, Postman folder *Phase 5 - Discounts* (log in as admin first).
  The apply path is exercised once booking lands (Phase 6).

## Migration

`V6__create_discounts.sql` — `discount` schema; `discount.discounts` (`uq_discount_code`),
`discount.discount_redemptions` (`uq_redemption_discount_booking`, indexes on `discount_id` and
`(discount_id, user_id)`). See [`../DATABASE.md`](../DATABASE.md).
