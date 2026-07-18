package com.moviebooking.ticket_booking.catalog.handler;

import com.moviebooking.ticket_booking.catalog.dto.MovieRequest;
import com.moviebooking.ticket_booking.catalog.dto.MovieResponse;
import com.moviebooking.ticket_booking.catalog.mapper.CatalogMapper;
import com.moviebooking.ticket_booking.catalog.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieRequestHandler {

    private final MovieService movieService;

    public MovieResponse create(MovieRequest request) {
        return CatalogMapper.toMovieResponse(movieService.create(request.title(), request.language(),
                request.genre(), request.durationMin(), request.certificate(), request.releaseDate()));
    }

    public MovieResponse update(Long id, MovieRequest request) {
        return CatalogMapper.toMovieResponse(movieService.update(id, request.title(), request.language(),
                request.genre(), request.durationMin(), request.certificate(), request.releaseDate()));
    }

    public void deactivate(Long id) {
        movieService.deactivate(id);
    }
}
