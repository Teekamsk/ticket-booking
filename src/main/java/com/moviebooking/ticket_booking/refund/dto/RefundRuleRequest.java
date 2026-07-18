package com.moviebooking.ticket_booking.refund.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RefundRuleRequest(

        @NotNull(message = "minMinutesBeforeShow is required")
        @Min(value = 0, message = "minMinutesBeforeShow must be at least 0")
        Integer minMinutesBeforeShow,

        @NotNull(message = "refundPercent is required")
        @Min(value = 0, message = "refundPercent must be between 0 and 100")
        @Max(value = 100, message = "refundPercent must be between 0 and 100")
        Integer refundPercent
) {
}
