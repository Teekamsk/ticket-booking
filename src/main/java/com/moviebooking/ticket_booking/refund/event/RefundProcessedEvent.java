package com.moviebooking.ticket_booking.refund.event;

/** Published after a refund is processed. Notification (Phase 9) listens to inform the customer. */
public record RefundProcessedEvent(Long userId, Long bookingId, String bookingRef, long amount) {
}
