package com.moviebooking.ticket_booking.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Notification tunables from {@code app.notification.*}. */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(Duration reminderWindow) {
}
