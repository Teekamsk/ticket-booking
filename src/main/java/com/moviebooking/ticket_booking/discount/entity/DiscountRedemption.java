package com.moviebooking.ticket_booking.discount.entity;

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

/** One recorded use of a discount. user_id / booking_id are cross-module ids (no FK). */
@Entity
@Table(name = "discount_redemptions", schema = "discount")
@Getter
@Setter
@NoArgsConstructor
public class DiscountRedemption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discount_id", nullable = false)
    private Discount discount;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;
}
