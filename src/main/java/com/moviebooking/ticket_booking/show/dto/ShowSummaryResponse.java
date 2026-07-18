package com.moviebooking.ticket_booking.show.dto;

import java.time.Instant;

/** A single show within a theatre group in search results. */
public record ShowSummaryResponse(
        Long id, Long movieId, String movieTitle, String movieCertificate, String movieLanguage, int durationMin,
        Long screenId, String screenName, Instant startTime, Instant endTime, String status
) {
}
