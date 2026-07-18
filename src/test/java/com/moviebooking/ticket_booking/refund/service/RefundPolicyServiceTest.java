package com.moviebooking.ticket_booking.refund.service;

import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;
import com.moviebooking.ticket_booking.refund.repository.RefundPolicyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPolicyServiceTest {

    @Mock
    private RefundPolicyRepository refundPolicyRepository;

    @InjectMocks
    private RefundPolicyService refundPolicyService;

    @Test
    void create_uniqueName_savesPolicyWithRules() {
        when(refundPolicyRepository.existsByNameIgnoreCase("Standard")).thenReturn(false);
        when(refundPolicyRepository.save(any(RefundPolicy.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundPolicy result = refundPolicyService.create("Standard", true,
                List.of(new RefundRuleSpec(1440, 100), new RefundRuleSpec(30, 0)));

        assertThat(result.getName()).isEqualTo("Standard");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getRules()).hasSize(2);
        assertThat(result.getRules()).allSatisfy(rule -> assertThat(rule.getPolicy()).isSameAs(result));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(refundPolicyRepository.existsByNameIgnoreCase("Standard")).thenReturn(true);

        assertThatThrownBy(() -> refundPolicyService.create("Standard", true, List.of(new RefundRuleSpec(30, 0))))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_duplicateThreshold_throwsBusinessRule() {
        when(refundPolicyRepository.existsByNameIgnoreCase("Standard")).thenReturn(false);

        assertThatThrownBy(() -> refundPolicyService.create("Standard", true,
                List.of(new RefundRuleSpec(60, 50), new RefundRuleSpec(60, 10))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void getOrThrow_missing_throwsNotFound() {
        when(refundPolicyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refundPolicyService.getOrThrow(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
