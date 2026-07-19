package com.moviebooking.ticket_booking.show.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ShowPriceRequest(

        @NotBlank(message = "seatType is required")
        @Size(max = 20, message = "seatType must be at most 20 characters")
        String seatType,

        @NotNull(message = "price is required")
        @Positive(message = "price (paise) must be positive")
        Long price
) {
}
