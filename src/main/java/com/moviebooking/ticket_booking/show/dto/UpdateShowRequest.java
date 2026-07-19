package com.moviebooking.ticket_booking.show.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;

import java.time.Instant;
import java.util.List;

/** All fields optional; provide startTime and/or prices. Validated further in the service. */
public record UpdateShowRequest(

        @Future(message = "startTime must be in the future")
        Instant startTime,

        @Valid
        List<ShowPriceRequest> prices
) {
}
