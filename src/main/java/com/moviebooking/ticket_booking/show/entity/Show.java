package com.moviebooking.ticket_booking.show.entity;

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

/**
 * A screening. Catalog/refund references are denormalized snapshots (plain ids + display fields,
 * no cross-schema FK) so all show reads stay within the show schema.
 */
@Entity
@Table(name = "shows", schema = "show")
@Getter
@Setter
@NoArgsConstructor
public class Show extends BaseEntity {

    // --- movie snapshot (catalog) ---
    @Column(name = "movie_id", nullable = false)
    private Long movieId;
    @Column(name = "movie_title", nullable = false, length = 200)
    private String movieTitle;
    @Column(name = "movie_language", nullable = false, length = 50)
    private String movieLanguage;
    @Column(name = "movie_certificate", nullable = false, length = 5)
    private String movieCertificate;
    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    // --- venue snapshot (catalog) ---
    @Column(name = "screen_id", nullable = false)
    private Long screenId;
    @Column(name = "screen_name", nullable = false, length = 50)
    private String screenName;
    @Column(name = "theatre_id", nullable = false)
    private Long theatreId;
    @Column(name = "theatre_name", nullable = false, length = 150)
    private String theatreName;
    @Column(name = "city_id", nullable = false)
    private Long cityId;
    @Column(name = "city_name", nullable = false, length = 100)
    private String cityName;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShowStatus status;

    /** Refund policy reference (refund module) — plain id, no cross-schema FK. */
    @Column(name = "refund_policy_id", nullable = false)
    private Long refundPolicyId;

    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShowPricing> pricing = new ArrayList<>();

    public void addPricing(ShowPricing p) {
        p.setShow(this);
        pricing.add(p);
    }

    public void clearPricing() {
        pricing.clear();
    }
}
