package com.moviebooking.ticket_booking.catalog.handler;

import com.moviebooking.ticket_booking.catalog.dto.CityResponse;
import com.moviebooking.ticket_booking.catalog.dto.MovieResponse;
import com.moviebooking.ticket_booking.catalog.mapper.CatalogMapper;
import com.moviebooking.ticket_booking.catalog.service.CityService;
import com.moviebooking.ticket_booking.catalog.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Public browse queries. Read-only, no authentication required. */
@Component
@RequiredArgsConstructor
public class CatalogBrowseHandler {

    private final CityService cityService;
    private final MovieService movieService;

    public List<CityResponse> listActiveCities() {
        return cityService.listActive().stream().map(CatalogMapper::toCityResponse).toList();
    }

    public List<MovieResponse> browseMovies(String query) {
        return movieService.browse(query).stream().map(CatalogMapper::toMovieResponse).toList();
    }
}
