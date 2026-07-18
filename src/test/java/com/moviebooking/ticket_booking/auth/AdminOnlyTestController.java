package com.moviebooking.ticket_booking.auth;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test-only admin-guarded route used to verify RBAC method security end-to-end. */
@RestController
class AdminOnlyTestController {

    @GetMapping("/api/v1/test/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    String adminOnly() {
        return "ok";
    }
}
