package com.moviebooking.ticket_booking.refund.service;

import com.moviebooking.ticket_booking.common.exception.BusinessRuleException;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import com.moviebooking.ticket_booking.common.exception.ResourceNotFoundException;
import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;
import com.moviebooking.ticket_booking.refund.entity.RefundRule;
import com.moviebooking.ticket_booking.refund.repository.RefundPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundPolicyService {

    private final RefundPolicyRepository refundPolicyRepository;

    @Transactional
    public RefundPolicy create(String name, boolean active, List<RefundRuleSpec> rules) {
        if (refundPolicyRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Refund policy '" + name + "' already exists");
        }
        validateRules(rules);
        RefundPolicy policy = new RefundPolicy();
        policy.setName(name);
        policy.setActive(active);
        rules.forEach(spec -> policy.addRule(toRule(spec)));
        RefundPolicy saved = refundPolicyRepository.save(policy);
        log.info("Created refund policy {} (id={}) with {} rules", saved.getName(), saved.getId(), rules.size());
        return saved;
    }

    @Transactional
    public RefundPolicy update(Long id, String name, boolean active, List<RefundRuleSpec> rules) {
        RefundPolicy policy = getOrThrow(id);
        if (!policy.getName().equalsIgnoreCase(name) && refundPolicyRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Refund policy '" + name + "' already exists");
        }
        validateRules(rules);
        policy.setName(name);
        policy.setActive(active);
        // Replace the whole rule set. Flush the removals before inserting so a reused threshold
        // does not transiently violate uq_refund_rule_policy_threshold within the same flush.
        policy.getRules().clear();
        refundPolicyRepository.flush();
        rules.forEach(spec -> policy.addRule(toRule(spec)));
        return policy;
    }

    @Transactional(readOnly = true)
    public List<RefundPolicy> listAll() {
        return refundPolicyRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public RefundPolicy getOrThrow(Long id) {
        return refundPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Refund policy " + id + " not found"));
    }

    private void validateRules(List<RefundRuleSpec> rules) {
        Set<Integer> thresholds = new HashSet<>();
        for (RefundRuleSpec spec : rules) {
            if (!thresholds.add(spec.minMinutesBeforeShow())) {
                throw new BusinessRuleException(
                        "Duplicate rule threshold: " + spec.minMinutesBeforeShow() + " minutes");
            }
        }
    }

    private RefundRule toRule(RefundRuleSpec spec) {
        RefundRule rule = new RefundRule();
        rule.setMinMinutesBeforeShow(spec.minMinutesBeforeShow());
        rule.setRefundPercent(spec.refundPercent());
        return rule;
    }
}
