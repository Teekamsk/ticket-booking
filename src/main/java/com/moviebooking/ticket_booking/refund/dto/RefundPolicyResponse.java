package com.moviebooking.ticket_booking.refund.dto;

import java.util.List;

public record RefundPolicyResponse(Long id, String name, boolean active, List<RefundRuleResponse> rules) {
}
