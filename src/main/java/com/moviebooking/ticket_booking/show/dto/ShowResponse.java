package com.moviebooking.ticket_booking.show.dto;

import java.time.Instant;
import java.util.List;

public record ShowResponse(
        Long id,
        Long movieId, String movieTitle, String movieLanguage, String movieCertificate, int durationMin,
        Long screenId, String screenName, Long theatreId, String theatreName, Long cityId, String cityName,
        Instant startTime, Instant endTime, String status, Long refundPolicyId,
        List<ShowPricingResponse> prices
) {
}
