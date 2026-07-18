package com.moviebooking.ticket_booking.refund.dto;

public record RefundRuleResponse(Long id, int minMinutesBeforeShow, int refundPercent) {
}
