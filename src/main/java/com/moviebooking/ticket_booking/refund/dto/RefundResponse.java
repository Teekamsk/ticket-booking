package com.moviebooking.ticket_booking.refund.dto;

import com.moviebooking.ticket_booking.refund.entity.RefundStatus;

import java.time.Instant;

public record RefundResponse(Long id, Long bookingId, long amount, RefundStatus status, Instant processedAt) {
}
