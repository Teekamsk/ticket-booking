package com.moviebooking.ticket_booking.discount.repository;

import com.moviebooking.ticket_booking.discount.entity.DiscountRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscountRedemptionRepository extends JpaRepository<DiscountRedemption, Long> {

    long countByDiscount_Id(Long discountId);

    long countByDiscount_IdAndUserId(Long discountId, Long userId);

    boolean existsByDiscount_IdAndBookingId(Long discountId, Long bookingId);

    void deleteByBookingId(Long bookingId);
}
