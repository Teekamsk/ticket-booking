package com.moviebooking.ticket_booking.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Simulated-gateway tunables from {@code app.payment.*}. failureRate 0.0 = always succeeds. */
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(Duration delay, double failureRate) {
}
