package com.moviebooking.ticket_booking.show.entity;

/** Availability of a single seat for a single show — the source of truth booking locks on. */
public enum ShowSeatStatus {
    AVAILABLE,
    HELD,
    BOOKED
}
