package com.moviebooking.ticket_booking.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CityRequest(

        @NotBlank(message = "City name is required")
        @Size(max = 100, message = "City name must be at most 100 characters")
        String name,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must be at most 100 characters")
        String state
) {
}
