package com.moviebooking.ticket_booking.discount.service;

import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.discount.entity.Discount;
import com.moviebooking.ticket_booking.discount.entity.DiscountType;
import com.moviebooking.ticket_booking.discount.entity.DiscountValueType;
import com.moviebooking.ticket_booking.discount.repository.DiscountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountService discountService;

    private final Instant from = Instant.now();
    private final Instant to = from.plus(Duration.ofDays(30));

    private DiscountCommand command(DiscountValueType type, long value, Instant validFrom, Instant validTo) {
        return new DiscountCommand("SAVE", type, value, null, 0, validFrom, validTo, null, null, true);
    }

    @Test
    void create_valid_savesPromo() {
        when(discountRepository.existsByCodeIgnoreCase("SAVE")).thenReturn(false);
        when(discountRepository.save(any(Discount.class))).thenAnswer(inv -> inv.getArgument(0));

        Discount result = discountService.create(command(DiscountValueType.PERCENT, 20, from, to));

        assertThat(result.getType()).isEqualTo(DiscountType.PROMO);
        assertThat(result.getValue()).isEqualTo(20);
    }

    @Test
    void create_duplicateCode_throwsConflict() {
        when(discountRepository.existsByCodeIgnoreCase("SAVE")).thenReturn(true);

        assertThatThrownBy(() -> discountService.create(command(DiscountValueType.PERCENT, 20, from, to)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_percentOutOfRange_throwsBusinessRule() {
        when(discountRepository.existsByCodeIgnoreCase("SAVE")).thenReturn(false);

        assertThatThrownBy(() -> discountService.create(command(DiscountValueType.PERCENT, 150, from, to)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_flatNonPositive_throwsBusinessRule() {
        when(discountRepository.existsByCodeIgnoreCase("SAVE")).thenReturn(false);

        assertThatThrownBy(() -> discountService.create(command(DiscountValueType.FLAT, 0, from, to)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void create_validToBeforeFrom_throwsBusinessRule() {
        when(discountRepository.existsByCodeIgnoreCase("SAVE")).thenReturn(false);

        assertThatThrownBy(() -> discountService.create(command(DiscountValueType.FLAT, 5000, to, from)))
                .isInstanceOf(BusinessRuleException.class);
    }
}
