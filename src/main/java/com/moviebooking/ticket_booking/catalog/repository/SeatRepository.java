package com.moviebooking.ticket_booking.catalog.repository;

import com.moviebooking.ticket_booking.catalog.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    boolean existsByScreenIdAndRowLabelIgnoreCase(Long screenId, String rowLabel);

    List<Seat> findByScreenIdOrderByRowLabelAscSeatNumberAsc(Long screenId);
}
