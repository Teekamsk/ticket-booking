package com.moviebooking.ticket_booking.catalog.dto;

import com.moviebooking.ticket_booking.catalog.entity.SeatType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SeatRowBlock(

        @NotBlank(message = "Row label is required")
        @Size(max = 3, message = "Row label must be at most 3 characters")
        String rowLabel,

        @NotNull(message = "Seat type is required")
        SeatType seatType,

        @NotNull(message = "Seat count is required")
        @Min(value = 1, message = "Seat count must be at least 1")
        @Max(value = 100, message = "Seat count must be at most 100")
        Integer count
) {
}
