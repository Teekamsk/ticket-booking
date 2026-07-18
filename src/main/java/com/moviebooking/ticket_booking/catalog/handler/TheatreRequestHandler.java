package com.moviebooking.ticket_booking.catalog.handler;

import com.moviebooking.ticket_booking.catalog.dto.CreateTheatreRequest;
import com.moviebooking.ticket_booking.catalog.dto.TheatreResponse;
import com.moviebooking.ticket_booking.catalog.dto.UpdateTheatreRequest;
import com.moviebooking.ticket_booking.catalog.mapper.CatalogMapper;
import com.moviebooking.ticket_booking.catalog.service.TheatreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TheatreRequestHandler {

    private final TheatreService theatreService;

    public TheatreResponse create(CreateTheatreRequest request) {
        return CatalogMapper.toTheatreResponse(
                theatreService.create(request.cityId(), request.name(), request.address()));
    }

    public TheatreResponse update(Long id, UpdateTheatreRequest request) {
        return CatalogMapper.toTheatreResponse(
                theatreService.update(id, request.name(), request.address()));
    }

    public void deactivate(Long id) {
        theatreService.deactivate(id);
    }
}
