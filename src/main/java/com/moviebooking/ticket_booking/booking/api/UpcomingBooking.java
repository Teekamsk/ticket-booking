package com.moviebooking.ticket_booking.booking.api;

import java.time.Instant;

/** A confirmed booking whose show is starting soon — for reminder notifications. */
public record UpcomingBooking(Long userId, Long bookingId, String bookingRef, String movieTitle, Instant startTime) {
}
