package com.moviebooking.ticket_booking.payment.dto;

import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import com.moviebooking.ticket_booking.payment.entity.PaymentStatus;

public record PaymentResponse(
        Long id, Long bookingId, long amount, PaymentMethod method, PaymentStatus status, String txnRef
) {
}
