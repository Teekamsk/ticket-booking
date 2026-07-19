package com.moviebooking.ticket_booking.discount.web;

import com.moviebooking.ticket_booking.discount.dto.DiscountRequest;
import com.moviebooking.ticket_booking.discount.dto.DiscountResponse;
import com.moviebooking.ticket_booking.discount.handler.DiscountRequestHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin management of PROMO discounts. All routes require ADMIN. */
@RestController
@RequestMapping("/api/v1/admin/discounts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDiscountController {

    private final DiscountRequestHandler discountRequestHandler;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountResponse create(@Valid @RequestBody DiscountRequest request) {
        return discountRequestHandler.create(request);
    }

    @PutMapping("/{id}")
    public DiscountResponse update(@PathVariable Long id, @Valid @RequestBody DiscountRequest request) {
        return discountRequestHandler.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long id) {
        discountRequestHandler.deactivate(id);
    }

    @GetMapping
    public List<DiscountResponse> list() {
        return discountRequestHandler.list();
    }

    @GetMapping("/{id}")
    public DiscountResponse get(@PathVariable Long id) {
        return discountRequestHandler.get(id);
    }
}
