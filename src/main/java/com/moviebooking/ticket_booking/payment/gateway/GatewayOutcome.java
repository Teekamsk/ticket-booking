package com.moviebooking.ticket_booking.payment.gateway;

/** Result of a simulated charge attempt. */
public record GatewayOutcome(boolean success, String txnRef) {
}
