package com.moviebooking.ticket_booking.booking.event;

/** Published after a booking is cancelled. Refund (Phase 8) listens to create the payout. */
public record BookingCancelledEvent(
        Long bookingId, String bookingRef, Long userId, Long cancellationId,
        int refundPercentApplied, long paidAmount
) {
}
