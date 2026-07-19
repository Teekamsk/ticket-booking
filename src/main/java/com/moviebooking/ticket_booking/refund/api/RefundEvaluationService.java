package com.moviebooking.ticket_booking.refund.api;

import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;
import com.moviebooking.ticket_booking.refund.service.RefundPolicyService;
import com.moviebooking.ticket_booking.refund.service.RefundRuleEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.OptionalInt;

/**
 * Published facade for evaluating a refund policy against a cancellation's time gap.
 * Empty result = the cancellation is earlier than any tier allows (caller treats as "not cancellable").
 */
@Service
@RequiredArgsConstructor
public class RefundEvaluationService {

    private final RefundPolicyService refundPolicyService;

    @Transactional(readOnly = true)
    public OptionalInt evaluate(Long policyId, long minutesBeforeShow) {
        RefundPolicy policy = refundPolicyService.getOrThrow(policyId);
        return RefundRuleEvaluator.match(policy, minutesBeforeShow)
                .map(rule -> OptionalInt.of(rule.getRefundPercent()))
                .orElseGet(OptionalInt::empty);
    }
}
