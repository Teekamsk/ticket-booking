package com.moviebooking.ticket_booking.catalog.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "movies", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class Movie extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(name = "duration_min", nullable = false)
    private int durationMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Certificate certificate;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
