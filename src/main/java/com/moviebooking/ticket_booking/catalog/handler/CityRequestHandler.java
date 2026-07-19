package com.moviebooking.ticket_booking.catalog.handler;

import com.moviebooking.ticket_booking.catalog.dto.CityRequest;
import com.moviebooking.ticket_booking.catalog.dto.CityResponse;
import com.moviebooking.ticket_booking.catalog.mapper.CatalogMapper;
import com.moviebooking.ticket_booking.catalog.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CityRequestHandler {

    private final CityService cityService;

    public CityResponse create(CityRequest request) {
        return CatalogMapper.toCityResponse(cityService.create(request.name(), request.state()));
    }

    public CityResponse update(Long id, CityRequest request) {
        return CatalogMapper.toCityResponse(cityService.update(id, request.name(), request.state()));
    }

    public void deactivate(Long id) {
        cityService.deactivate(id);
    }
}
