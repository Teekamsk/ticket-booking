package com.moviebooking.ticket_booking.show.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateShowRequest(

        @NotNull(message = "movieId is required")
        Long movieId,

        @NotNull(message = "screenId is required")
        Long screenId,

        @NotNull(message = "startTime is required")
        @Future(message = "startTime must be in the future")
        Instant startTime,

        @NotNull(message = "refundPolicyId is required")
        Long refundPolicyId,

        @NotEmpty(message = "At least one seat-type price is required")
        @Valid
        List<ShowPriceRequest> prices
) {
}
