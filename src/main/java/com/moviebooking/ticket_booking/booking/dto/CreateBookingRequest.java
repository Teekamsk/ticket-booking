package com.moviebooking.ticket_booking.booking.dto;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(

        @NotNull(message = "holdId is required")
        Long holdId,

        String discountCode
) {
}
