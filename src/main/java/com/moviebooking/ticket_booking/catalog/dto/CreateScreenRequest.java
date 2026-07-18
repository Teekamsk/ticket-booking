package com.moviebooking.ticket_booking.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateScreenRequest(

        @NotNull(message = "Theatre id is required")
        Long theatreId,

        @NotBlank(message = "Screen name is required")
        @Size(max = 50, message = "Screen name must be at most 50 characters")
        String name
) {
}
