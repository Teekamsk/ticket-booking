package com.moviebooking.ticket_booking.catalog.api;

/** Published cross-module view of a screen and its venue chain (for denormalization). */
public record ScreenLocation(Long screenId, String screenName,
                             Long theatreId, String theatreName,
                             Long cityId, String cityName) {
}
