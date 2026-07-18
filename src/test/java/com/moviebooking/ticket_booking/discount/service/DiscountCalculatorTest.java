package com.moviebooking.ticket_booking.discount.service;

import com.moviebooking.ticket_booking.discount.entity.Discount;
import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountCalculatorTest {

    private Discount discount(DiscountValueType type, long value, Long cap) {
        Discount d = new Discount();
        d.setDiscountType(type);
        d.setValue(value);
        d.setMaxDiscountAmount(cap);
        return d;
    }

    @Test
    void percent_appliesCap() {
        assertThat(DiscountCalculator.compute(discount(DiscountValueType.PERCENT, 20, 10000L), 100000))
                .isEqualTo(10000);
    }

    @Test
    void percent_uncapped() {
        assertThat(DiscountCalculator.compute(discount(DiscountValueType.PERCENT, 20, null), 100000))
                .isEqualTo(20000);
    }

    @Test
    void flat_returnsValue() {
        assertThat(DiscountCalculator.compute(discount(DiscountValueType.FLAT, 5000, null), 100000))
                .isEqualTo(5000);
    }

    @Test
    void neverExceedsOrderAmount() {
        assertThat(DiscountCalculator.compute(discount(DiscountValueType.FLAT, 5000, null), 3000))
                .isEqualTo(3000);
    }
}
