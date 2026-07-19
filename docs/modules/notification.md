# Module: `notification` (Phase 9) — the communication service

A **multi-channel communication service**, not an email-only notifier. It reacts to domain events and
delivers messages over pluggable per-channel senders (EMAIL, SMS, PUSH — all currently log-stubbed).
No HTTP surface (internal module). Schema: `notification`.

## Architecture

```
BookingConfirmedEvent  ─┐
BookingCancelledEvent  ─┼─▶ @Async @TransactionalEventListener(AFTER_COMMIT)  ─▶ CommunicationService.dispatch(...)
RefundProcessedEvent   ─┘                                                              │
ReminderScheduler (@Scheduled) ────────────────────────────────────────────────────────┘
                                                                                        ▼
                                            for each channel → ChannelSenderRegistry.forChannel(c) → ChannelSender.send()
                                                              → persist Notification (SENT / FAILED)
```

- **`ChannelSender`** — interface (`channel()`, `send()`); implementations `EmailChannelSender`,
  `SmsChannelSender`, `PushChannelSender` (each logs). **Add a channel by dropping in a new
  implementation** — `ChannelSenderRegistry` auto-discovers all `ChannelSender` beans and maps them
  by channel.
- **`CommunicationService.dispatch(userId, bookingId, type, payload, channels)`** — persists one
  `Notification` per channel (PENDING → SENT/FAILED), delivering via the resolved sender. Channel
  selection is passed by the caller (per-user comm-preferences are out of scope; default = EMAIL+PUSH).
- **Listeners** — `BookingNotificationListener` (CONFIRMED/CANCELLED), `RefundNotificationListener`
  (REFUND_PROCESSED). All `@Async("notificationExecutor")` + `AFTER_COMMIT`, so delivery never blocks
  or breaks the originating transaction; a rolled-back booking sends nothing.
- **`ReminderScheduler`** — `@Scheduled`; asks `booking.api.BookingReminderService` for confirmed
  bookings whose show starts within `app.notification.reminder-window` (default 24h) and sends a
  one-time `SHOW_REMINDER` per booking (deduped via `existsByBookingIdAndType`).

## Entity

`notifications`: `user_id`, `booking_id` (nullable, cross-module id), `type`
(BOOKING_CONFIRMED/BOOKING_CANCELLED/REFUND_PROCESSED/SHOW_REMINDER), `channel` (EMAIL/SMS/PUSH),
`payload`, `status` (PENDING/SENT/FAILED), `sent_at`. Persisted for audit.

## Key decisions

- **Kept the module name `notification`** (schema/plan consistency) but architected it as a
  channel-based **communication service** per the requirement — pluggable senders, multi-channel
  dispatch, extensible without touching the dispatcher.
- **Default channels = EMAIL + PUSH.** `SmsChannelSender` is wired and pluggable but unused by default
  (would be driven by user preferences, out of scope).
- **Everything is stubbed via logs** — no real email/SMS/push is sent. Swap a sender implementation to
  integrate a real provider later.
- Cross-module coupling is **event contracts only** (`booking.event.*`, `refund.event.*`) plus the
  `booking.api` reminder query — no repositories of other modules.

## How to test

- **Automated:** `./gradlew test` — `CommunicationServiceTest` (per-channel persist + SENT/FAILED),
  `NotificationEventIntegrationTest` (pay → BOOKING_CONFIRMED×2; cancel → refund + BOOKING_CANCELLED×2
  + REFUND_PROCESSED×2, across the async chain), `ReminderSchedulerIntegrationTest` (one reminder per
  channel + dedup).
- **Manual:** run the booking→payment→cancel flow and watch the `[EMAIL]`/`[PUSH]` log lines; inspect
  `notification.notifications`.

## Migration

`V10__create_notifications.sql` — `notification` schema; `notification.notifications` with indexes on
`user_id` and `(booking_id, type)`. See [`../DATABASE.md`](../DATABASE.md).
