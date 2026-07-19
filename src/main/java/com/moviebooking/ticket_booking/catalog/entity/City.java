package com.moviebooking.ticket_booking.catalog.entity;

import com.moviebooking.ticket_booking.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cities", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
public class City extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
