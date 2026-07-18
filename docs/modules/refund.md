# Module: `refund` (Phase 3 — policy config; processing in Phase 8)

Owns configurable refund policies and their tiered rules. Phase 3 delivers admin CRUD + the
rule-evaluation logic. **Refund *processing*** (the `Refund` entity, evaluating a policy against a
cancellation, simulated payout) is deferred to Phase 8. Schema: `refund`.

## Endpoints — `/api/v1/admin/refund-policies` 🛡 ADMIN (`AdminRefundPolicyController`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/` | Create policy + rules (201) |
| PUT | `/{id}` | Update policy; **replaces the whole rule set** |
| GET | `/` | List all policies (rules included) |
| GET | `/{id}` | Get one policy |

## Layers (`com.moviebooking.ticket_booking.refund`)

- **entity** — `RefundPolicy` (name unique, `is_active`, `@OneToMany` rules with
  `cascade=ALL, orphanRemoval=true`), `RefundRule` (`policy_id` FK intra-schema,
  `min_minutes_before_show`, `refund_percent`).
- **repository** — `RefundPolicyRepository`; list/get use `@EntityGraph("rules")` so rules are
  loaded before response mapping (open-in-view is off).
- **service** — `RefundPolicyService` (create/update/list/get, uniqueness + duplicate-threshold
  checks). `RefundRuleSpec` is the web-agnostic command. `RefundRuleEvaluator` (static) does the
  matching, used by Phase 8; no endpoint.
- **dto / mapper / handler / web** — request/response records with validation, `RefundMapper`,
  `RefundPolicyRequestHandler`, `AdminRefundPolicyController`.

## Rule evaluation

For a cancellation `gap` minutes before showtime, the matched rule is the one with the **largest
`minMinutesBeforeShow ≤ gap`**. Example set `(1440→100%, 120→50%, 30→0%)`:

| gap (min) | matched | refund |
|---|---|---|
| 5000 | 1440 | 100% |
| 1440 | 1440 | 100% |
| 200 | 120 | 50% |
| 30 | 30 | 0% |
| 20 | — | none (earlier than any tier → Phase 8 treats as not refundable) |

## Key behaviours & decisions

- **PUT replaces the full rule set.** Rules are cleared then re-added; the removals are flushed
  before the inserts so reusing a threshold (e.g. keeping `1440`) doesn't transiently violate
  `uq_refund_rule_policy_threshold`.
- **A policy requires ≥1 rule** (`@NotEmpty`); thresholds must be unique within a policy
  (service check → `422`, plus a DB unique constraint as the safety net).
- `refundPercent` 0–100, `minMinutesBeforeShow` ≥ 0 (validated on the DTO).
- No DELETE endpoint (per DESIGN); `active` is settable via create/update (defaults true).
- **`Show` will reference a policy by `refund_policy_id`** (a plain id column, no cross-schema FK)
  in Phase 4 — this is why refund-policy config lands before `show`.

## How to test

- **Automated:** `./gradlew test` — `RefundRuleEvaluatorTest` (matching across gaps, boundaries,
  no-match), `RefundPolicyServiceTest` (create/duplicate-name/duplicate-threshold/not-found),
  `AdminRefundPolicyControllerTest` (validation/status, standalone), `RefundPolicyIntegrationTest`
  (CRUD with rule replacement, RBAC 401/403, duplicate name `409`, duplicate threshold `422`).
- **Manual:** `./gradlew bootRun`, Postman folder *Phase 3 - Refund Policies* (log in as admin first).

## Migration

`V4__create_refund_policies.sql` — creates the `refund` schema, `refund.refund_policies`
(`uq_refund_policy_name`) and `refund.refund_rules` (`uq_refund_rule_policy_threshold`,
index on `policy_id`). See [`../DATABASE.md`](../DATABASE.md).
