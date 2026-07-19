package com.moviebooking.ticket_booking.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateScreenRequest(

        @NotBlank(message = "Screen name is required")
        @Size(max = 50, message = "Screen name must be at most 50 characters")
        String name
) {
}
