package com.moviebooking.ticket_booking.discount.mapper;

import com.moviebooking.ticket_booking.discount.dto.DiscountResponse;
import com.moviebooking.ticket_booking.discount.entity.Discount;

/** Entity → response DTO mapping for discounts. */
public final class DiscountMapper {

    private DiscountMapper() {
    }

    public static DiscountResponse toResponse(Discount d) {
        return new DiscountResponse(d.getId(), d.getType(), d.getCode(), d.getDiscountType(), d.getValue(),
                d.getMaxDiscountAmount(), d.getMinOrderAmount(), d.getValidFrom(), d.getValidTo(),
                d.getMaxTotalUses(), d.getMaxUsesPerUser(), d.isActive());
    }
}
