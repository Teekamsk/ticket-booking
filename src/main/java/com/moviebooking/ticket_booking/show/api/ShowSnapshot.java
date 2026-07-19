package com.moviebooking.ticket_booking.show.api;

import com.moviebooking.ticket_booking.show.entity.ShowStatus;

import java.time.Instant;

/** Cross-module snapshot of a show, used by booking for denormalization and cancellation checks. */
public record ShowSnapshot(
        Long showId, String movieTitle, String screenName, String theatreName, String cityName,
        Instant startTime, Instant endTime, Long refundPolicyId, ShowStatus status
) {
}
