package com.moviebooking.ticket_booking.booking.entity;

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

/** A user's temporary claim on one or more ShowSeats (tracked via show_seats.hold_id). */
@Entity
@Table(name = "seat_holds", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
public class SeatHold extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }
}
