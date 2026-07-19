package com.moviebooking.ticket_booking.show.service;

import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowSeat;

import java.util.List;

/** A show plus its seats, for building the live seat map (price comes from the show's pricing). */
public record SeatMap(Show show, List<ShowSeat> seats) {
}
