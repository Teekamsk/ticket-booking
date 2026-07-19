package com.moviebooking.ticket_booking.catalog.entity;

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

@Entity
@Table(name = "screens", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class Screen extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    @Column(nullable = false, length = 50)
    private String name;

    /** Derived from seats, kept for display. Maintained on seat creation. */
    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
