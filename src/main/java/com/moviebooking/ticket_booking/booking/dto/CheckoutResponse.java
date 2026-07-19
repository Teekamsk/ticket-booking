package com.moviebooking.ticket_booking.booking.dto;

import java.time.Instant;
import java.util.List;

public record CheckoutResponse(
        Long holdId, Instant expiresAt, long secondsRemaining, long totalAmount,
        String discountCode, long discountAmount, long payableAmount, List<SeatLineResponse> seats
) {
}
