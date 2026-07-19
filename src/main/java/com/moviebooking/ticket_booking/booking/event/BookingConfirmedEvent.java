package com.moviebooking.ticket_booking.booking.event;

/** Published after a booking is confirmed. Notification (Phase 9) listens AFTER_COMMIT. */
public record BookingConfirmedEvent(Long bookingId, Long userId, String bookingRef) {
}
