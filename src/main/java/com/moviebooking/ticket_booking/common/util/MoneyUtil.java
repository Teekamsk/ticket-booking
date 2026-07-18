package com.moviebooking.ticket_booking.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money is stored as BIGINT paise everywhere. These helpers convert to/from rupees for I/O only. */
public final class MoneyUtil {

    private static final BigDecimal PAISE_PER_RUPEE = BigDecimal.valueOf(100);

    private MoneyUtil() {
    }

    public static long rupeesToPaise(BigDecimal rupees) {
        return rupees.multiply(PAISE_PER_RUPEE).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal paiseToRupees(long paise) {
        return BigDecimal.valueOf(paise).divide(PAISE_PER_RUPEE);
    }

    public static String format(long paise) {
        return "₹" + paiseToRupees(paise).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
