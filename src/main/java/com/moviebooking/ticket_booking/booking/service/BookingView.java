package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.entity.Booking;

import java.util.List;

public record BookingView(Booking booking, List<SeatLine> seats) {
}
