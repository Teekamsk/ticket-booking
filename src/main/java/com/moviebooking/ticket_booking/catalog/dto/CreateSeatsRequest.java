package com.moviebooking.ticket_booking.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateSeatsRequest(

        @NotEmpty(message = "At least one row is required")
        @Valid
        List<SeatRowBlock> rows
) {
}
