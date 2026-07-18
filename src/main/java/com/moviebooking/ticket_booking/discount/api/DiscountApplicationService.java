package com.moviebooking.ticket_booking.discount.api;

import com.moviebooking.ticket_booking.discount.entity.Discount;
import com.moviebooking.ticket_booking.discount.entity.DiscountRedemption;
import com.moviebooking.ticket_booking.discount.entity.DiscountType;
import com.moviebooking.ticket_booking.discount.exception.DiscountNotApplicableException;
import com.moviebooking.ticket_booking.discount.repository.DiscountRedemptionRepository;
import com.moviebooking.ticket_booking.discount.repository.DiscountRepository;
import com.moviebooking.ticket_booking.discount.service.DiscountCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Published facade the booking module uses to apply promo codes. Usage is counted at booking
 * confirmation ({@link #recordRedemption}) and returned on cancellation ({@link #releaseRedemption}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountApplicationService {

    private final DiscountRepository discountRepository;
    private final DiscountRedemptionRepository redemptionRepository;

    /** Validates eligibility and computes the discount, or throws {@link DiscountNotApplicableException}. */
    @Transactional(readOnly = true)
    public DiscountResult validateAndCompute(String code, Long userId, long orderAmount) {
        Discount discount = discountRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new DiscountNotApplicableException("Invalid discount code"));
        if (discount.getType() != DiscountType.PROMO || !discount.isActive()) {
            throw new DiscountNotApplicableException("Invalid discount code");
        }
        Instant now = Instant.now();
        if (now.isBefore(discount.getValidFrom()) || now.isAfter(discount.getValidTo())) {
            throw new DiscountNotApplicableException("Discount code is expired or not yet valid");
        }
        if (orderAmount < discount.getMinOrderAmount()) {
            throw new DiscountNotApplicableException("Order does not meet the minimum amount for this code");
        }
        if (discount.getMaxTotalUses() != null
                && redemptionRepository.countByDiscount_Id(discount.getId()) >= discount.getMaxTotalUses()) {
            throw new DiscountNotApplicableException("Discount code usage limit reached");
        }
        if (discount.getMaxUsesPerUser() != null
                && redemptionRepository.countByDiscount_IdAndUserId(discount.getId(), userId) >= discount.getMaxUsesPerUser()) {
            throw new DiscountNotApplicableException("You have already used this discount the maximum number of times");
        }
        long amount = DiscountCalculator.compute(discount, orderAmount);
        return new DiscountResult(discount.getId(), discount.getCode(), amount);
    }

    /** Records a use. Idempotent per (discount, booking). Called when a booking is confirmed. */
    @Transactional
    public void recordRedemption(Long discountId, Long userId, Long bookingId, long amount) {
        if (redemptionRepository.existsByDiscount_IdAndBookingId(discountId, bookingId)) {
            return;
        }
        Discount discount = discountRepository.findById(discountId)
                .orElseThrow(() -> new DiscountNotApplicableException("Discount no longer exists"));
        DiscountRedemption redemption = new DiscountRedemption();
        redemption.setDiscount(discount);
        redemption.setUserId(userId);
        redemption.setBookingId(bookingId);
        redemption.setDiscountAmount(amount);
        redemptionRepository.save(redemption);
        log.info("Recorded redemption of discount {} by user {} on booking {}", discountId, userId, bookingId);
    }

    /** Returns a use to the pool (e.g. on booking cancellation). */
    @Transactional
    public void releaseRedemption(Long bookingId) {
        redemptionRepository.deleteByBookingId(bookingId);
    }
}
