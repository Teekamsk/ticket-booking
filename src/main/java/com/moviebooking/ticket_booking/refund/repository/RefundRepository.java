package com.moviebooking.ticket_booking.refund.repository;

import com.moviebooking.ticket_booking.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    boolean existsByBookingId(Long bookingId);

    Optional<Refund> findByBookingId(Long bookingId);
}
