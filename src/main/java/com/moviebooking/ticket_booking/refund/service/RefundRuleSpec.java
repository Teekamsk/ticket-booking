package com.moviebooking.ticket_booking.refund.service;

/** Service-level command for one refund tier (keeps the service web-DTO agnostic). */
public record RefundRuleSpec(int minMinutesBeforeShow, int refundPercent) {
}
