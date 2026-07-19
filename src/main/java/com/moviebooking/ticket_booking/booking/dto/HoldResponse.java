package com.moviebooking.ticket_booking.booking.dto;

import java.time.Instant;
import java.util.List;

public record HoldResponse(
        Long holdId, Long showId, String status, Instant expiresAt, long secondsRemaining,
        long totalAmount, List<SeatLineResponse> seats
) {
}
