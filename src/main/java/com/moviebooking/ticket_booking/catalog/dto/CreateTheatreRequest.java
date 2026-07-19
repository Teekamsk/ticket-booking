package com.moviebooking.ticket_booking.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTheatreRequest(

        @NotNull(message = "City id is required")
        Long cityId,

        @NotBlank(message = "Theatre name is required")
        @Size(max = 150, message = "Theatre name must be at most 150 characters")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address
) {
}
