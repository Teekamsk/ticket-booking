package com.moviebooking.ticket_booking.discount.dto;

import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Create/update a PROMO discount. Type-specific value ranges are checked in the service. */
public record DiscountRequest(

        @NotBlank(message = "code is required")
        @Size(max = 30, message = "code must be at most 30 characters")
        String code,

        @NotNull(message = "discountType is required")
        DiscountValueType discountType,

        @NotNull(message = "value is required")
        @Positive(message = "value must be positive")
        Long value,

        @Positive(message = "maxDiscountAmount must be positive")
        Long maxDiscountAmount,

        @NotNull(message = "minOrderAmount is required")
        @PositiveOrZero(message = "minOrderAmount must not be negative")
        Long minOrderAmount,

        @NotNull(message = "validFrom is required")
        Instant validFrom,

        @NotNull(message = "validTo is required")
        Instant validTo,

        @Positive(message = "maxTotalUses must be positive")
        Integer maxTotalUses,

        @Positive(message = "maxUsesPerUser must be positive")
        Integer maxUsesPerUser,

        Boolean active
) {
    public boolean activeOrDefault() {
        return active == null || active;
    }
}
