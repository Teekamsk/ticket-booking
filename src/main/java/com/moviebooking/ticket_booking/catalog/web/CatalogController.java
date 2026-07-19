package com.moviebooking.ticket_booking.catalog.web;

import com.moviebooking.ticket_booking.catalog.dto.CityResponse;
import com.moviebooking.ticket_booking.catalog.dto.MovieResponse;
import com.moviebooking.ticket_booking.catalog.handler.CatalogBrowseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public catalog browse. No authentication required. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogBrowseHandler catalogBrowseHandler;

    @GetMapping("/cities")
    public List<CityResponse> listCities() {
        return catalogBrowseHandler.listActiveCities();
    }

    @GetMapping("/movies")
    public List<MovieResponse> browseMovies(@RequestParam(required = false) String q) {
        return catalogBrowseHandler.browseMovies(q);
    }
}
