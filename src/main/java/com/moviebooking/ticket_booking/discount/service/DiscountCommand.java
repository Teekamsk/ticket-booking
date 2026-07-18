package com.moviebooking.ticket_booking.discount.service;

import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;

import java.time.Instant;

/** Web-agnostic command for creating/updating a PROMO discount. */
public record DiscountCommand(
        String code,
        DiscountValueType discountType,
        long value,
        Long maxDiscountAmount,
        long minOrderAmount,
        Instant validFrom,
        Instant validTo,
        Integer maxTotalUses,
        Integer maxUsesPerUser,
        boolean active
) {
}
