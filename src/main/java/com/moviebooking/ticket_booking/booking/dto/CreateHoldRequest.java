package com.moviebooking.ticket_booking.booking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateHoldRequest(

        @NotNull(message = "showId is required")
        Long showId,

        @NotEmpty(message = "At least one seatId is required")
        List<@NotNull(message = "seatId must not be null") Long> seatIds
) {
}
