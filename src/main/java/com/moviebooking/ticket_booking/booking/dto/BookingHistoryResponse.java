package com.moviebooking.ticket_booking.booking.dto;

import java.util.List;

public record BookingHistoryResponse(
        List<BookingSummaryResponse> content, int page, int size, long totalElements, int totalPages
) {
}
