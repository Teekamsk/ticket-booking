package com.moviebooking.ticket_booking.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilTest {

    @Test
    void rupeesToPaise_convertsAndRoundsHalfUp() {
        assertThat(MoneyUtil.rupeesToPaise(new BigDecimal("250.00"))).isEqualTo(25000L);
        assertThat(MoneyUtil.rupeesToPaise(new BigDecimal("199.995"))).isEqualTo(20000L);
    }

    @Test
    void paiseToRupees_convertsBack() {
        assertThat(MoneyUtil.paiseToRupees(25000L)).isEqualByComparingTo("250.00");
    }

    @Test
    void format_rendersRupeesWithTwoDecimals() {
        assertThat(MoneyUtil.format(25000L)).isEqualTo("₹250.00");
        assertThat(MoneyUtil.format(20050L)).isEqualTo("₹200.50");
    }
}
