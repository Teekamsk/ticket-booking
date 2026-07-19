package com.moviebooking.ticket_booking.catalog.api;

/** Published cross-module view of a movie. Other modules depend on this, not on the entity. */
public record MovieSummary(Long id, String title, String language, String certificate, int durationMin) {
}
