package com.moviebooking.ticket_booking.booking.dto;

import java.time.Instant;
import java.util.List;

public record BookingResponse(
        Long id, String bookingRef, Long showId, String movieTitle, String screenName, String theatreName,
        Instant startTime, String status, long totalAmount, long discountAmount, long payableAmount,
        Instant confirmedAt, Instant cancelledAt, List<SeatLineResponse> seats
) {
}
