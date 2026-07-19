package com.moviebooking.ticket_booking.show.dto;

import java.time.Instant;
import java.util.List;

public record SeatMapResponse(
        Long showId, String movieTitle, String screenName, String theatreName, Instant startTime,
        List<SeatMapEntry> seats
) {
}
