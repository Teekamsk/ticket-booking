package com.moviebooking.ticket_booking.discount.api;

import com.moviebooking.ticket_booking.discount.entity.Discount;
import com.moviebooking.ticket_booking.discount.entity.DiscountType;
import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;
import com.moviebooking.ticket_booking.discount.exception.DiscountNotApplicableException;
import com.moviebooking.ticket_booking.discount.repository.DiscountRedemptionRepository;
import com.moviebooking.ticket_booking.discount.repository.DiscountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountApplicationServiceTest {

    @Mock
    private DiscountRepository discountRepository;
    @Mock
    private DiscountRedemptionRepository redemptionRepository;

    @InjectMocks
    private DiscountApplicationService service;

    private Discount promo() {
        Discount d = new Discount();
        d.setType(DiscountType.PROMO);
        d.setCode("SAVE20");
        d.setDiscountType(DiscountValueType.PERCENT);
        d.setValue(20);
        d.setMaxDiscountAmount(10000L);
        d.setMinOrderAmount(50000);
        d.setValidFrom(Instant.now().minus(Duration.ofDays(1)));
        d.setValidTo(Instant.now().plus(Duration.ofDays(1)));
        d.setActive(true);
        return d;
    }

    @Test
    void validateAndCompute_eligible_returnsCappedAmount() {
        when(discountRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(promo()));

        DiscountResult result = service.validateAndCompute("SAVE20", 1L, 100000);

        assertThat(result.discountAmount()).isEqualTo(10000);
        assertThat(result.code()).isEqualTo("SAVE20");
    }

    @Test
    void validateAndCompute_unknownCode_throws() {
        when(discountRepository.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndCompute("NOPE", 1L, 100000))
                .isInstanceOf(DiscountNotApplicableException.class);
    }

    @Test
    void validateAndCompute_expired_throws() {
        Discount expired = promo();
        expired.setValidTo(Instant.now().minus(Duration.ofHours(1)));
        when(discountRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.validateAndCompute("SAVE20", 1L, 100000))
                .isInstanceOf(DiscountNotApplicableException.class);
    }

    @Test
    void validateAndCompute_belowMinOrder_throws() {
        when(discountRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(promo()));

        assertThatThrownBy(() -> service.validateAndCompute("SAVE20", 1L, 10000))
                .isInstanceOf(DiscountNotApplicableException.class);
    }

    @Test
    void validateAndCompute_perUserLimitReached_throws() {
        Discount d = promo();
        d.setMaxUsesPerUser(1);
        when(discountRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(d));
        when(redemptionRepository.countByDiscount_IdAndUserId(any(), any())).thenReturn(1L);

        assertThatThrownBy(() -> service.validateAndCompute("SAVE20", 1L, 100000))
                .isInstanceOf(DiscountNotApplicableException.class);
    }

    @Test
    void releaseRedemption_deletesByBooking() {
        service.releaseRedemption(999L);
        verify(redemptionRepository).deleteByBookingId(999L);
    }
}
