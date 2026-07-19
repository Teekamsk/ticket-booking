package com.moviebooking.ticket_booking.booking.repository;

import com.moviebooking.ticket_booking.booking.entity.Cancellation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationRepository extends JpaRepository<Cancellation, Long> {

    boolean existsByBookingId(Long bookingId);
}
