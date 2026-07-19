package com.moviebooking.ticket_booking.show.service;

/** Service-level command for one seat-type price (web-DTO agnostic). */
public record PriceItem(String seatType, long price) {
}
