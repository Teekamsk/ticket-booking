package com.moviebooking.ticket_booking.notification.entity;

/** Communication channels. Senders are pluggable per channel; all are log-stubbed for now. */
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH
}
