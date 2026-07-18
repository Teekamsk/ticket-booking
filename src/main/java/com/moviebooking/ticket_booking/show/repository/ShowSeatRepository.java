package com.moviebooking.ticket_booking.show.repository;

import com.moviebooking.ticket_booking.show.entity.ShowSeat;
import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowIdOrderByRowLabelAscSeatNumberAsc(Long showId);

    boolean existsByShowIdAndStatusNot(Long showId, ShowSeatStatus status);

    boolean existsByShowIdAndStatus(Long showId, ShowSeatStatus status);
}
