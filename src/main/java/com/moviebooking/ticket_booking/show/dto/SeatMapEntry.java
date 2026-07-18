package com.moviebooking.ticket_booking.show.dto;

public record SeatMapEntry(
        Long showSeatId, Long seatId, String rowLabel, int seatNumber, String seatType, String status, long price
) {
}
