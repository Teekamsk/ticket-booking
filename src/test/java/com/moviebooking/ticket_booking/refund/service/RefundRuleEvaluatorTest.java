package com.moviebooking.ticket_booking.refund.service;

import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;
import com.moviebooking.ticket_booking.refund.entity.RefundRule;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RefundRuleEvaluatorTest {

    private RefundPolicy policyWithRules() {
        RefundPolicy policy = new RefundPolicy();
        policy.setName("Standard");
        policy.setActive(true);
        policy.addRule(rule(1440, 100));
        policy.addRule(rule(120, 50));
        policy.addRule(rule(30, 0));
        return policy;
    }

    private RefundRule rule(int minMinutes, int percent) {
        RefundRule r = new RefundRule();
        r.setMinMinutesBeforeShow(minMinutes);
        r.setRefundPercent(percent);
        return r;
    }

    @Test
    void match_wellAboveTopTier_picksHighestThreshold() {
        Optional<RefundRule> match = RefundRuleEvaluator.match(policyWithRules(), 5000);
        assertThat(match).get().extracting(RefundRule::getRefundPercent).isEqualTo(100);
    }

    @Test
    void match_exactlyOnThreshold_isInclusive() {
        assertThat(RefundRuleEvaluator.match(policyWithRules(), 1440)).get()
                .extracting(RefundRule::getRefundPercent).isEqualTo(100);
        assertThat(RefundRuleEvaluator.match(policyWithRules(), 30)).get()
                .extracting(RefundRule::getRefundPercent).isEqualTo(0);
    }

    @Test
    void match_betweenTiers_picksLargerThresholdNotExceedingGap() {
        assertThat(RefundRuleEvaluator.match(policyWithRules(), 200)).get()
                .extracting(RefundRule::getRefundPercent).isEqualTo(50);
    }

    @Test
    void match_belowAllThresholds_returnsEmpty() {
        assertThat(RefundRuleEvaluator.match(policyWithRules(), 20)).isEmpty();
    }
}
