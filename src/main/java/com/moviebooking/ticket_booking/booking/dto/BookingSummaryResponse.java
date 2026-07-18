package com.moviebooking.ticket_booking.booking.dto;

import java.time.Instant;

public record BookingSummaryResponse(
        Long id, String bookingRef, String status, String movieTitle, Instant startTime, long payableAmount
) {
}
