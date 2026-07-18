package com.moviebooking.ticket_booking.refund.mapper;

import com.moviebooking.ticket_booking.refund.dto.RefundPolicyResponse;
import com.moviebooking.ticket_booking.refund.dto.RefundRuleResponse;
import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;

import java.util.Comparator;
import java.util.List;

/** Entity → response DTO mapping for refund policies. */
public final class RefundMapper {

    private RefundMapper() {
    }

    public static RefundPolicyResponse toPolicyResponse(RefundPolicy policy) {
        List<RefundRuleResponse> rules = policy.getRules().stream()
                .sorted(Comparator.comparingInt(r -> r.getMinMinutesBeforeShow()))
                .map(r -> new RefundRuleResponse(r.getId(), r.getMinMinutesBeforeShow(), r.getRefundPercent()))
                .toList();
        return new RefundPolicyResponse(policy.getId(), policy.getName(), policy.isActive(), rules);
    }
}
