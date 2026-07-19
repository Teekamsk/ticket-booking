package com.moviebooking.ticket_booking.show.entity;

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

/** Price (paise) for one seat type at one show. */
@Entity
@Table(name = "show_pricing", schema = "show")
@Getter
@Setter
@NoArgsConstructor
public class ShowPricing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "seat_type", nullable = false, length = 20)
    private String seatType;

    @Column(nullable = false)
    private long price;
}
