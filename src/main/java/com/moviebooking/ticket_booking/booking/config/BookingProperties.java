package com.moviebooking.ticket_booking.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Booking tunables bound from {@code app.booking.*}. */
@ConfigurationProperties(prefix = "app.booking")
public record BookingProperties(Duration holdTtl) {
}
