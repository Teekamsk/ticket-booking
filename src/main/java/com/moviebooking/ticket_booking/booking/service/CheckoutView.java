package com.moviebooking.ticket_booking.booking.service;

import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import com.moviebooking.ticket_booking.show.api.LockedSeat;

import java.util.List;

public record CheckoutView(
        SeatHold hold, List<LockedSeat> seats, long totalAmount,
        String discountCode, long discountAmount, long payableAmount
) {
}
