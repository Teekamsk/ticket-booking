package com.moviebooking.ticket_booking.notification.repository;

import com.moviebooking.ticket_booking.notification.entity.Notification;
import com.moviebooking.ticket_booking.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByBookingIdAndType(Long bookingId, NotificationType type);

    long countByBookingIdAndType(Long bookingId, NotificationType type);
}
