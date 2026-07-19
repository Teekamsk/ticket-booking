package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.show.api.LockedSeat;

import java.util.List;

public record HoldView(SeatHold hold, List<LockedSeat> seats) {
}
