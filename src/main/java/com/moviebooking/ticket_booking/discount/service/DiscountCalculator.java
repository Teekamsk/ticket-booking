package com.moviebooking.ticket_booking.discount.service;

import com.moviebooking.ticket_booking.discount.entity.Discount;

/** Computes the discount amount (paise) for an order. Never exceeds the order total. */
public final class DiscountCalculator {

    private DiscountCalculator() {
    }

    public static long compute(Discount discount, long orderAmount) {
        long raw = switch (discount.getDiscountType()) {
            case PERCENT -> {
                long amount = orderAmount * discount.getValue() / 100;
                if (discount.getMaxDiscountAmount() != null) {
                    amount = Math.min(amount, discount.getMaxDiscountAmount());
                }
                yield amount;
            }
            case FLAT -> discount.getValue();
        };
        return Math.min(raw, orderAmount);
    }
}
