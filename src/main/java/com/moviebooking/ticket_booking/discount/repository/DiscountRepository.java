package com.moviebooking.ticket_booking.discount.repository;

import com.moviebooking.ticket_booking.discount.entity.Discount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    boolean existsByCodeIgnoreCase(String code);

    Optional<Discount> findByCodeIgnoreCase(String code);

    List<Discount> findAllByOrderByCodeAsc();
}
