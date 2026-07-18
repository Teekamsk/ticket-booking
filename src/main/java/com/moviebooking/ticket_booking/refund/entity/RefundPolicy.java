package com.moviebooking.ticket_booking.refund.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "refund_policies", schema = "refund")
@Getter
@NoArgsConstructor
public class RefundPolicy extends BaseEntity {

    @Setter
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Setter
    @Column(name = "is_active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefundRule> rules = new ArrayList<>();

    public void addRule(RefundRule rule) {
        rule.setPolicy(this);
        rules.add(rule);
    }
}
