package com.moviebooking.ticket_booking.booking.dto;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(

        @Size(max = 255, message = "reason must be at most 255 characters")
        String reason
) {
}
