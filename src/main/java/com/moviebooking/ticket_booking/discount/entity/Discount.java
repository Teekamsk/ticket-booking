package com.moviebooking.ticket_booking.discount.entity;

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

/** A discount. Single table with a {@code type} discriminator; Phase 5 manages PROMO only. */
@Entity
@Table(name = "discounts", schema = "discount")
@Getter
@Setter
@NoArgsConstructor
public class Discount extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    /** Code the user enters (PROMO). Unique; null for OFFER. */
    @Column(length = 30, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountValueType discountType;

    /** Percent (0–100) when PERCENT, else paise when FLAT. */
    @Column(nullable = false)
    private long value;

    /** Cap for PERCENT discounts (paise). Null = uncapped. */
    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "min_order_amount", nullable = false)
    private long minOrderAmount;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    /** Null = unlimited. */
    @Column(name = "max_total_uses")
    private Integer maxTotalUses;

    /** Null = unlimited. */
    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
