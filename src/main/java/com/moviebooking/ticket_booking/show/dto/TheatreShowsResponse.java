package com.moviebooking.ticket_booking.show.dto;

import java.util.List;

/** Search results grouped by theatre. */
public record TheatreShowsResponse(
        Long theatreId, String theatreName, Long cityId, String cityName, List<ShowSummaryResponse> shows
) {
}
