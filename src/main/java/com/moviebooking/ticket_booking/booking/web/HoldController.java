package com.moviebooking.ticket_booking.booking.web;

import com.moviebooking.ticket_booking.booking.dto.CheckoutResponse;
import com.moviebooking.ticket_booking.booking.dto.CreateHoldRequest;
import com.moviebooking.ticket_booking.booking.dto.HoldResponse;
import com.moviebooking.ticket_booking.booking.handler.HoldRequestHandler;
import com.moviebooking.ticket_booking.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Seat holds. Customer-only. */
@RestController
@RequestMapping("/api/v1/holds")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class HoldController {

    private final HoldRequestHandler holdRequestHandler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody CreateHoldRequest request) {
        return holdRequestHandler.create(user.userId(), request);
    }

    @GetMapping("/{id}")
    public HoldResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return holdRequestHandler.get(user.userId(), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        holdRequestHandler.release(user.userId(), id);
    }

    @GetMapping("/{id}/checkout")
    public CheckoutResponse checkout(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                     @RequestParam(required = false) String discountCode) {
        return holdRequestHandler.checkout(user.userId(), id, discountCode);
    }
}
