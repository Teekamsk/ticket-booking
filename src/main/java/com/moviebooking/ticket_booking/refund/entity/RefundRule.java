package com.moviebooking.ticket_booking.refund.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One refund tier: at least {@code minMinutesBeforeShow} before start, refund {@code refundPercent}%. */
@Entity
@Table(name = "refund_rules", schema = "refund")
@Getter
@Setter
@NoArgsConstructor
public class RefundRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private RefundPolicy policy;

    @Column(name = "min_minutes_before_show", nullable = false)
    private int minMinutesBeforeShow;

    @Column(name = "refund_percent", nullable = false)
    private int refundPercent;
}
