package com.moviebooking.ticket_booking.booking.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Audit record of a booking cancellation, with the refund percent snapshot (payout is Phase 8). */
@Entity
@Table(name = "cancellations", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
public class Cancellation extends BaseEntity {

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "cancelled_by", nullable = false)
    private Long cancelledBy;

    @Column(length = 255)
    private String reason;

    @Column(name = "refund_percent_applied", nullable = false)
    private int refundPercentApplied;
}
