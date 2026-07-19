package com.moviebooking.ticket_booking.booking.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Order-level record. Show fields are denormalized so booking history is self-contained. */
@Entity
@Table(name = "bookings", schema = "booking")
@Getter
@Setter
@NoArgsConstructor
public class Booking extends BaseEntity {

    @Column(name = "booking_ref", nullable = false, unique = true, length = 12)
    private String bookingRef;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "hold_id", nullable = false)
    private Long holdId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    @Column(name = "discount_id")
    private Long discountId;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount;

    @Column(name = "payable_amount", nullable = false)
    private long payableAmount;

    // --- show snapshot (for self-contained history) ---
    @Column(name = "movie_title", nullable = false, length = 200)
    private String movieTitle;
    @Column(name = "screen_name", nullable = false, length = 50)
    private String screenName;
    @Column(name = "theatre_name", nullable = false, length = 150)
    private String theatreName;
    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    @Column(name = "refund_policy_id", nullable = false)
    private Long refundPolicyId;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    public void addTicket(Ticket ticket) {
        ticket.setBooking(this);
        tickets.add(ticket);
    }
}
