package com.moviebooking.ticket_booking.refund.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A simulated refund payout against the original payment. Cross-module ids are plain (no FK). */
@Entity
@Table(name = "refunds", schema = "refund")
@Getter
@Setter
@NoArgsConstructor
public class Refund extends BaseEntity {

    @Column(name = "cancellation_id", nullable = false)
    private Long cancellationId;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "processed_at")
    private Instant processedAt;
}
