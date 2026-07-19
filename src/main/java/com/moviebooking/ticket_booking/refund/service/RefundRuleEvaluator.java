package com.moviebooking.ticket_booking.refund.service;

import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;
import com.moviebooking.ticket_booking.refund.entity.RefundRule;

import java.util.Comparator;
import java.util.Optional;

/**
 * Evaluates a refund policy for a given time gap. The matching rule is the one with the largest
 * {@code minMinutesBeforeShow} that is still ≤ the gap. No match ⇒ the cancellation is earlier than
 * any tier allows (caller decides how to treat that — typically "not refundable / not cancellable").
 * Used by refund processing in Phase 8; no HTTP endpoint.
 */
public final class RefundRuleEvaluator {

    private RefundRuleEvaluator() {
    }

    public static Optional<RefundRule> match(RefundPolicy policy, long minutesBeforeShow) {
        return policy.getRules().stream()
                .filter(rule -> rule.getMinMinutesBeforeShow() <= minutesBeforeShow)
                .max(Comparator.comparingInt(RefundRule::getMinMinutesBeforeShow));
    }
}
