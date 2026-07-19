package com.moviebooking.ticket_booking.discount.dto;

import com.moviebooking.ticket_booking.discount.entity.DiscountType;
import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;

import java.time.Instant;

public record DiscountResponse(
        Long id, DiscountType type, String code, DiscountValueType discountType, long value,
        Long maxDiscountAmount, long minOrderAmount, Instant validFrom, Instant validTo,
        Integer maxTotalUses, Integer maxUsesPerUser, boolean active
) {
}
