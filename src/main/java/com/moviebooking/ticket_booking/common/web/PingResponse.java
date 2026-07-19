package com.moviebooking.ticket_booking.common.web;

import java.time.Instant;

/** Liveness probe response. */
public record PingResponse(String status, String service, Instant timestamp) {
}
