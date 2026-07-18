package com.moviebooking.ticket_booking.show.repository;

import com.moviebooking.ticket_booking.show.entity.ShowSeat;
import com.moviebooking.ticket_booking.show.entity.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowIdOrderByRowLabelAscSeatNumberAsc(Long showId);

    boolean existsByShowIdAndStatusNot(Long showId, ShowSeatStatus status);

    boolean existsByShowIdAndStatus(Long showId, ShowSeatStatus status);

    List<ShowSeat> findByHoldId(Long holdId);

    /** Pessimistic row lock (SELECT … FOR UPDATE), ordered by id to avoid deadlocks. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ShowSeat s where s.show.id = :showId and s.seatId in :seatIds order by s.id")
    List<ShowSeat> lockSeats(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);

    @Modifying
    @Query("update ShowSeat s set s.status = com.moviebooking.ticket_booking.show.entity.ShowSeatStatus.HELD, "
            + "s.holdId = :holdId where s.id in :ids")
    void markHeld(@Param("ids") List<Long> ids, @Param("holdId") Long holdId);

    @Modifying
    @Query("update ShowSeat s set s.status = com.moviebooking.ticket_booking.show.entity.ShowSeatStatus.BOOKED "
            + "where s.id in :ids")
    void markBooked(@Param("ids") List<Long> ids);

    @Modifying
    @Query("update ShowSeat s set s.status = com.moviebooking.ticket_booking.show.entity.ShowSeatStatus.AVAILABLE, "
            + "s.holdId = null where s.holdId = :holdId "
            + "and s.status = com.moviebooking.ticket_booking.show.entity.ShowSeatStatus.HELD")
    void releaseByHold(@Param("holdId") Long holdId);

    @Modifying
    @Query("update ShowSeat s set s.status = com.moviebooking.ticket_booking.show.entity.ShowSeatStatus.AVAILABLE, "
            + "s.holdId = null where s.show.id = :showId and s.seatId in :seatIds")
    void releaseByShowAndSeats(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);
}
