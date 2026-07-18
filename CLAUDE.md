# Movie Ticket Booking System

A modular-monolith movie ticket booking system: multiple cities/theatres/screens, seat-level
booking with TTL holds, pricing tiers, discount codes, simulated payment, refunds under
configurable policies, and non-blocking notifications. Correct serialization of concurrent
bookings (no double-allocation) is a core requirement.

**The authoritative design lives in [`docs/DESIGN.md`](docs/DESIGN.md).** Read it before making
any change. Entities, modules, class responsibilities, REST APIs, and the core hold→pay→confirm
flow are all specified there — follow it, do not re-invent it.

## Stack

- Java 17, Spring Boot 4.1, Spring Data JPA, Bean Validation, Lombok
- PostgreSQL, Gradle (Kotlin DSL)
- Base package: `com.moviebooking.ticket_booking`
- Build: `./gradlew build` · Test: `./gradlew test`

## Architecture

Package-by-module (modular monolith): `com.moviebooking.ticket_booking.<module>`, each module
owning its own layers. Modules: `auth`, `catalog`, `show`, `booking`, `payment`, `discount`,
`refund`, `notification`, `common`.

Per-module layering — every request flows through these layers in order:

```
Controller  →  RequestHandler  →  Service  →  Repository  →  Entity
```

- **Controller** — HTTP mapping only. Delegates to the RequestHandler. No business logic.
- **RequestHandler** — one per request. Orchestrates cross-service calls, maps between
  request/response DTOs, and owns error-response mapping for that request. Keeps controllers thin.
- **Service** — business logic, transactions. Owns its own module's repositories only.
- **Repository** — Spring Data JPA. Concurrency locks (`@Lock(PESSIMISTIC_WRITE)`) live here.
- **Entity** — JPA entities. Never leave the service/persistence layer (see rule 5).

Cross-module calls go **service → service**, never repository-of-another-module.

## Development Guidelines

These are binding. Apply them to every change.

1. **Follow the architecture.** Respect the module boundaries and the layer order above. Keep
   patterns consistent with existing code — same package layout, naming, and conventions.
2. **Accurate and concise code.** Write the minimum correct code that solves the problem. No
   dead code, no speculative abstractions, no unused parameters.
3. **Limit verbosity; avoid over-commenting.** Code should read for itself. Comment only
   non-obvious *why*, never restate *what* the code plainly does.
4. **Do not assume requirements or architecture-level decisions.** If a requirement is
   ambiguous, or a change touches a key design decision (entity shape, concurrency strategy,
   module boundaries, API contracts, pricing/refund rules), **stop and ask the developer.**
   Never silently pick a direction.
5. **Separate request/response objects per request.** Every endpoint has its own request and
   response DTOs. **Never expose or accept JPA entities in the API layer** — always map
   entity ↔ DTO (mapping done in the RequestHandler).
6. **Logging.** Log at meaningful boundaries (request entry/exit in handlers, state
   transitions in services, external/simulated calls, error paths). Use SLF4J (`@Slf4j`).
   Never log secrets, passwords, tokens, or full payment details. Prefer parameterized logging
   (`log.info("hold {} expired", holdId)`), not string concatenation.
7. **Validation with proper messaging.** Validate all input with Bean Validation
   (`@Valid`, `@NotNull`, `@Size`, etc.) on request DTOs. Every constraint carries a clear,
   user-facing `message`. Business-rule validation (seat availability, hold expiry, discount
   eligibility) lives in the service and raises a domain exception.
8. **One RequestHandler per request.** Cross-service orchestration and error-response mapping
   for a request belong in its RequestHandler — not in the controller or scattered in services.
9. **Proper error responses.** Errors use RFC-7807 Problem Details via the module's
   handler + `GlobalExceptionHandler` in `common`. Every error has an accurate HTTP status
   and a clear message. Conventions: `409` seat conflict, `410` expired hold, `400` validation,
   `401/403` auth, `404` not found, `422` business-rule rejection.
10. **Build module by module, each independently testable.** Deliver one module at a time per
    the plan in [`docs/DEVELOPMENT_PLAN.md`](docs/DEVELOPMENT_PLAN.md). A module is not "done"
    until its APIs are runnable and its unit/web/(concurrency) tests pass. Do not start the next
    module until the current one is green.
11. **Keep module architecture docs current.** Every module has a living doc at
    `docs/modules/<module>.md`. When you create or change a module, create/update its doc in the
    same change — entities, endpoints, key decisions, and how to test it. Docs and code ship
    together, never after.

## Testing

- Framework: JUnit 5 (`useJUnitPlatform`). Mockito for mocks; Spring Boot test slices.
- **Service unit tests** — mock repositories; cover happy path, every domain exception, and
  boundary conditions (hold at TTL edge, discount cap, refund-rule matching).
- **Concurrency tests are mandatory for the booking flow.** Prove no double-allocation: multiple
  threads holding/booking the same `ShowSeat` must serialize — exactly one succeeds, the rest
  get `409`. This is the system's headline correctness property; do not merge booking changes
  without it.
- **Web-layer tests** (`@WebMvcTest`) — assert status codes, validation messages, and the
  RFC-7807 error body; confirm entities are never serialized (DTOs only).
- Name tests `methodUnderTest_condition_expectedResult`. Arrange–Act–Assert; one behavior
  per test. Assert on messages and codes, not just status.
- Add/extend tests with every behavioral change. Run `./gradlew test` before considering a
  change done; report failures honestly with output.

## When in doubt

Re-read `docs/DESIGN.md`. If it doesn't answer the question, **ask the developer** (rule 4)
rather than guessing.