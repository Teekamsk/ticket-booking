package com.moviebooking.ticket_booking.show.web;

import com.moviebooking.ticket_booking.show.dto.CreateShowRequest;
import com.moviebooking.ticket_booking.show.dto.ShowResponse;
import com.moviebooking.ticket_booking.show.dto.UpdateShowRequest;
import com.moviebooking.ticket_booking.show.handler.AdminShowRequestHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin show scheduling. All routes require ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/shows")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminShowController {

    private final AdminShowRequestHandler adminShowRequestHandler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse create(@Valid @RequestBody CreateShowRequest request) {
        return adminShowRequestHandler.create(request);
    }

    @PutMapping("/{id}")
    public ShowResponse update(@PathVariable Long id, @Valid @RequestBody UpdateShowRequest request) {
        return adminShowRequestHandler.update(id, request);
    }

    @PostMapping("/{id}/cancel")
    public ShowResponse cancel(@PathVariable Long id) {
        return adminShowRequestHandler.cancel(id);
    }
}
