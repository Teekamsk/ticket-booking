package com.moviebooking.ticket_booking.catalog.dto;

import com.moviebooking.ticket_booking.catalog.entity.Certificate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MovieRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Language is required")
        @Size(max = 50, message = "Language must be at most 50 characters")
        String language,

        @NotBlank(message = "Genre is required")
        @Size(max = 50, message = "Genre must be at most 50 characters")
        String genre,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 1000, message = "Duration must be at most 1000 minutes")
        Integer durationMin,

        @NotNull(message = "Certificate is required")
        Certificate certificate,

        @NotNull(message = "Release date is required")
        LocalDate releaseDate
) {
}
