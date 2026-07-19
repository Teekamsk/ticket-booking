package com.moviebooking.ticket_booking.catalog.mapper;

import com.moviebooking.ticket_booking.catalog.dto.CityResponse;
import com.moviebooking.ticket_booking.catalog.dto.MovieResponse;
import com.moviebooking.ticket_booking.catalog.dto.ScreenResponse;
import com.moviebooking.ticket_booking.catalog.dto.SeatResponse;
import com.moviebooking.ticket_booking.catalog.dto.TheatreResponse;
import com.moviebooking.ticket_booking.catalog.entity.City;
import com.moviebooking.ticket_booking.catalog.entity.Movie;
import com.moviebooking.ticket_booking.catalog.entity.Screen;
import com.moviebooking.ticket_booking.catalog.entity.Seat;
import com.moviebooking.ticket_booking.catalog.entity.Theatre;

/** Entity → response DTO mapping. Keeps entities out of the API layer. */
public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CityResponse toCityResponse(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getState(), city.isActive());
    }

    public static TheatreResponse toTheatreResponse(Theatre theatre) {
        return new TheatreResponse(theatre.getId(), theatre.getCity().getId(),
                theatre.getName(), theatre.getAddress(), theatre.isActive());
    }

    public static ScreenResponse toScreenResponse(Screen screen) {
        return new ScreenResponse(screen.getId(), screen.getTheatre().getId(),
                screen.getName(), screen.getTotalSeats(), screen.isActive());
    }

    public static SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(seat.getId(), seat.getScreen().getId(),
                seat.getRowLabel(), seat.getSeatNumber(), seat.getSeatType(), seat.isActive());
    }

    public static MovieResponse toMovieResponse(Movie movie) {
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getLanguage(), movie.getGenre(),
                movie.getDurationMin(), movie.getCertificate(), movie.getReleaseDate(), movie.isActive());
    }
}
