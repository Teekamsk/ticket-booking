package com.moviebooking.ticket_booking.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** JWT signing configuration bound from {@code app.jwt.*}. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration) {
}
