package com.moviebooking.ticket_booking.refund.handler;

import com.moviebooking.ticket_booking.refund.dto.RefundPolicyRequest;
import com.moviebooking.ticket_booking.refund.dto.RefundPolicyResponse;
import com.moviebooking.ticket_booking.refund.mapper.RefundMapper;
import com.moviebooking.ticket_booking.refund.service.RefundPolicyService;
import com.moviebooking.ticket_booking.refund.service.RefundRuleSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RefundPolicyRequestHandler {

    private final RefundPolicyService refundPolicyService;

    public RefundPolicyResponse create(RefundPolicyRequest request) {
        return RefundMapper.toPolicyResponse(
                refundPolicyService.create(request.name(), request.activeOrDefault(), toSpecs(request)));
    }

    public RefundPolicyResponse update(Long id, RefundPolicyRequest request) {
        return RefundMapper.toPolicyResponse(
                refundPolicyService.update(id, request.name(), request.activeOrDefault(), toSpecs(request)));
    }

    public List<RefundPolicyResponse> list() {
        return refundPolicyService.listAll().stream().map(RefundMapper::toPolicyResponse).toList();
    }

    public RefundPolicyResponse get(Long id) {
        return RefundMapper.toPolicyResponse(refundPolicyService.getOrThrow(id));
    }

    private List<RefundRuleSpec> toSpecs(RefundPolicyRequest request) {
        return request.rules().stream()
                .map(r -> new RefundRuleSpec(r.minMinutesBeforeShow(), r.refundPercent()))
                .toList();
    }
}
