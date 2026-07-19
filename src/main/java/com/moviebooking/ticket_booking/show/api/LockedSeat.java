package com.moviebooking.ticket_booking.show.api;

import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;

/** A ShowSeat row (locked or read) exposed to the booking module, with its current price. */
public record LockedSeat(
        Long showSeatId, Long seatId, String rowLabel, int seatNumber, String seatType,
        ShowSeatStatus status, Long holdId, long price
) {
}
