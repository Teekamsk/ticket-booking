package com.moviebooking.ticket_booking.booking.dto;

public record SeatLineResponse(Long seatId, String rowLabel, int seatNumber, String seatType, long price, String status) {
}
