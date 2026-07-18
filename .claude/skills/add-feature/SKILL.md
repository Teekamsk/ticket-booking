---
name: add-feature
description: Use when adding a new REST endpoint or feature to the ticket-booking system — scaffolds the change through the module's layered architecture (Controller → RequestHandler → Service → Repository → Entity) with request/response DTOs, validation, logging, and error handling per the project guidelines.
---

# Add a feature (layered, module-owned)

Follow this whenever adding or changing an endpoint. Match existing code in the target module.
The design source of truth is `docs/DESIGN.md` — the endpoint, its access level, and its entities
are almost certainly already specified there. Do not invent contracts that contradict it.

## Before writing code

1. Locate the owning module (`auth`, `catalog`, `show`, `booking`, `payment`, `discount`,
   `refund`, `notification`). One feature = one owning module.
2. Confirm the API contract against `docs/DESIGN.md` §4 (path, method, access role, payload).
3. **If anything is ambiguous or touches a design-level decision, stop and ask the developer.**
   Do not assume entity shapes, concurrency strategy, or contract details.

## Layers to create/touch (in order)

1. **Request DTO** — `dto/<Action>Request.java`. Bean Validation on every field with a clear
   `message`. Never accept an entity.
2. **Response DTO** — `dto/<Action>Response.java`. Never return an entity.
3. **Repository** — add query methods as needed. Concurrency locks
   (`@Lock(LockModeType.PESSIMISTIC_WRITE)`, `FOR UPDATE`, ordered fetch) live here.
4. **Service** — business logic + `@Transactional`. Owns only this module's repositories.
   Cross-module needs go service → service. Raise domain exceptions for rule violations.
5. **RequestHandler** — one per request. Orchestrates service calls, maps entity ↔ DTO,
   and owns error-response mapping for this request. Keeps the controller thin.
6. **Controller** — `@RestController` mapping only: bind path/body (`@Valid`), delegate to the
   RequestHandler, return the response DTO. No logic here.

## Cross-cutting (always)

- **Validation:** `@Valid` on the request; every constraint has a user-facing message.
  Business-rule checks (availability, expiry, eligibility) go in the service as domain exceptions.
- **Logging:** `@Slf4j`, parameterized. Log request boundaries in the handler and state
  transitions in the service. Never log secrets/tokens/full payment data.
- **Errors:** map to RFC-7807 Problem Details with the correct status — `409` seat conflict,
  `410` expired hold, `400` validation, `401/403` auth, `404` not found, `422` business rejection.
  Register new domain exceptions with `common/GlobalExceptionHandler`.
- **Concurrency (booking module):** lock `ShowSeat` rows ordered by id, verify AVAILABLE/expired,
  fail closed (nothing held on conflict). See DESIGN §5.

## Definition of done

- Request/response DTOs exist; no entity crosses the API boundary.
- Validation messages present; error paths return proper codes + messages.
- Service unit tests + (for booking) a concurrency test. `./gradlew test` passes.
- Code is concise, minimally commented, consistent with the module.