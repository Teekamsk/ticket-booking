---
name: write-tests
description: Use when writing or extending tests for the ticket-booking system — establishes JUnit 5 conventions, service unit tests with mocked repositories, mandatory concurrency tests for the booking flow, and web-layer tests asserting status codes and RFC-7807 error bodies.
---

# Writing tests

Framework: JUnit 5 (`useJUnitPlatform`), Mockito, Spring Boot test slices. Run `./gradlew test`.

## Conventions

- Name tests `methodUnderTest_condition_expectedResult`.
- Arrange–Act–Assert; one behavior per test.
- Assert on **messages and status/error codes**, not just HTTP status alone.
- Add or extend tests with every behavioral change.

## Service unit tests

- Mock repositories; unit-test the service in isolation.
- Cover: happy path, **every domain exception**, and boundary conditions
  (hold at TTL edge, discount cap hit, refund-rule matching by `min_minutes_before_show`).
- Verify transactional side effects (status transitions, snapshots) via mock interactions.

## Concurrency tests (mandatory for booking)

The headline correctness property is **no double-allocation**. Do not merge booking changes
without a test proving it.

- Spin up multiple threads that hold/book the **same `ShowSeat`** concurrently
  (e.g. `ExecutorService` + `CountDownLatch` to release together).
- Assert exactly **one** succeeds and the rest fail with `409`.
- Assert the final `ShowSeat` state is consistent (single HELD/BOOKED owner).
- Prefer a real DB (or Testcontainers Postgres) so `FOR UPDATE` locking is actually exercised.

## Web-layer tests (`@WebMvcTest`)

- Assert status codes and the **RFC-7807 Problem Details** error body shape.
- Assert validation failures return `400` with the configured field messages.
- Confirm responses serialize **DTOs only** — entities must never appear in the payload.

## Don't

- Don't test getters/setters or framework wiring.
- Don't assert on log output as a behavioral contract.
- Don't weaken a concurrency test to make it pass — fix the code.