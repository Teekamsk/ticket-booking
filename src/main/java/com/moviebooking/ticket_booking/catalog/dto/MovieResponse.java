package com.moviebooking.ticket_booking.catalog.dto;

import com.moviebooking.ticket_booking.catalog.entity.Certificate;

import java.time.LocalDate;

public record MovieResponse(Long id, String title, String language, String genre, int durationMin,
                            Certificate certificate, LocalDate releaseDate, boolean active) {
}
