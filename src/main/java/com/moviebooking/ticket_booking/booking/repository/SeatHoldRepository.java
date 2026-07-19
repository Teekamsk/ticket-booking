package com.moviebooking.ticket_booking.booking.repository;

import com.moviebooking.ticket_booking.booking.entity.HoldStatus;
import com.moviebooking.ticket_booking.booking.entity.SeatHold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    List<SeatHold> findByStatusAndExpiresAtBefore(HoldStatus status, Instant time);
}
