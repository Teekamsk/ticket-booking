package com.moviebooking.ticket_booking.catalog.web;

import com.moviebooking.ticket_booking.catalog.dto.CityRequest;
import com.moviebooking.ticket_booking.catalog.dto.CityResponse;
import com.moviebooking.ticket_booking.catalog.dto.CreateScreenRequest;
import com.moviebooking.ticket_booking.catalog.dto.CreateSeatsRequest;
import com.moviebooking.ticket_booking.catalog.dto.CreateSeatsResponse;
import com.moviebooking.ticket_booking.catalog.dto.CreateTheatreRequest;
import com.moviebooking.ticket_booking.catalog.dto.MovieRequest;
import com.moviebooking.ticket_booking.catalog.dto.MovieResponse;
import com.moviebooking.ticket_booking.catalog.dto.ScreenResponse;
import com.moviebooking.ticket_booking.catalog.dto.SeatResponse;
import com.moviebooking.ticket_booking.catalog.dto.TheatreResponse;
import com.moviebooking.ticket_booking.catalog.dto.UpdateScreenRequest;
import com.moviebooking.ticket_booking.catalog.dto.UpdateTheatreRequest;
import com.moviebooking.ticket_booking.catalog.handler.CityRequestHandler;
import com.moviebooking.ticket_booking.catalog.handler.MovieRequestHandler;
import com.moviebooking.ticket_booking.catalog.handler.ScreenRequestHandler;
import com.moviebooking.ticket_booking.catalog.handler.TheatreRequestHandler;
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

/** Admin catalog management. All routes require ADMIN. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final CityRequestHandler cityRequestHandler;
    private final TheatreRequestHandler theatreRequestHandler;
    private final ScreenRequestHandler screenRequestHandler;
    private final MovieRequestHandler movieRequestHandler;

    // --- Cities ---

    @PostMapping("/cities")
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse createCity(@Valid @RequestBody CityRequest request) {
        return cityRequestHandler.create(request);
    }

    @PutMapping("/cities/{id}")
    public CityResponse updateCity(@PathVariable Long id, @Valid @RequestBody CityRequest request) {
        return cityRequestHandler.update(id, request);
    }

    @DeleteMapping("/cities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCity(@PathVariable Long id) {
        cityRequestHandler.deactivate(id);
    }

    // --- Theatres ---

    @PostMapping("/theatres")
    @ResponseStatus(HttpStatus.CREATED)
    public TheatreResponse createTheatre(@Valid @RequestBody CreateTheatreRequest request) {
        return theatreRequestHandler.create(request);
    }

    @PutMapping("/theatres/{id}")
    public TheatreResponse updateTheatre(@PathVariable Long id, @Valid @RequestBody UpdateTheatreRequest request) {
        return theatreRequestHandler.update(id, request);
    }

    @DeleteMapping("/theatres/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateTheatre(@PathVariable Long id) {
        theatreRequestHandler.deactivate(id);
    }

    // --- Screens & seats ---

    @PostMapping("/screens")
    @ResponseStatus(HttpStatus.CREATED)
    public ScreenResponse createScreen(@Valid @RequestBody CreateScreenRequest request) {
        return screenRequestHandler.create(request);
    }

    @PutMapping("/screens/{id}")
    public ScreenResponse updateScreen(@PathVariable Long id, @Valid @RequestBody UpdateScreenRequest request) {
        return screenRequestHandler.update(id, request);
    }

    @PostMapping("/screens/{id}/seats")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSeatsResponse addSeats(@PathVariable Long id, @Valid @RequestBody CreateSeatsRequest request) {
        return screenRequestHandler.addSeats(id, request);
    }

    @GetMapping("/screens/{id}/seats")
    public List<SeatResponse> listSeats(@PathVariable Long id) {
        return screenRequestHandler.listSeats(id);
    }

    // --- Movies ---

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@Valid @RequestBody MovieRequest request) {
        return movieRequestHandler.create(request);
    }

    @PutMapping("/movies/{id}")
    public MovieResponse updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest request) {
        return movieRequestHandler.update(id, request);
    }

    @DeleteMapping("/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateMovie(@PathVariable Long id) {
        movieRequestHandler.deactivate(id);
    }
}
