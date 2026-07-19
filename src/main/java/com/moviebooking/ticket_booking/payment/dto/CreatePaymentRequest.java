package com.moviebooking.ticket_booking.payment.dto;

import com.moviebooking.ticket_booking.payment.entity.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePaymentRequest(

        @NotNull(message = "bookingId is required")
        Long bookingId,

        @NotNull(message = "method is required")
        PaymentMethod method,

        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 64, message = "idempotencyKey must be at most 64 characters")
        String idempotencyKey
) {
}
