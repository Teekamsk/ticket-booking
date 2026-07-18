package com.moviebooking.ticket_booking.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables scheduled jobs (hold-expiry sweeper Phase 6, reminders Phase 9). */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}
