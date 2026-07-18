package com.moviebooking.ticket_booking.show.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One (show × seat). This row is the concurrency anchor booking locks on (Phase 6); its status is
 * the single source of truth for availability. Seat details are snapshotted from catalog.
 */
@Entity
@Table(name = "show_seats", schema = "show")
@Getter
@Setter
@NoArgsConstructor
public class ShowSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    /** Physical seat reference (catalog) — plain id, no cross-schema FK. */
    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(name = "row_label", nullable = false, length = 3)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowSeatStatus status;

    /** Set while HELD (booking module, Phase 6) — plain id, no cross-schema FK. */
    @Column(name = "hold_id")
    private Long holdId;

    @Version
    @Column(nullable = false)
    private long version;
}
