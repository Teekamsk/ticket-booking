package com.moviebooking.ticket_booking.refund.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RefundPolicyRequest(

        @NotBlank(message = "Policy name is required")
        @Size(max = 100, message = "Policy name must be at most 100 characters")
        String name,

        /** Optional; defaults to true when omitted. */
        Boolean active,

        @NotEmpty(message = "At least one refund rule is required")
        @Valid
        List<RefundRuleRequest> rules
) {
    public boolean activeOrDefault() {
        return active == null || active;
    }
}
