package com.moviebooking.ticket_booking.common.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Public liveness probe — confirms the app is up without touching the database. */
@RestController
@RequestMapping("/api/v1/ping")
public class PingController {

    @GetMapping
    public PingResponse ping() {
        return new PingResponse("UP", "ticket-booking", Instant.now());
    }
}
