package com.moviebooking.ticket_booking.show.repository;

import com.moviebooking.ticket_booking.show.entity.Show;
import com.moviebooking.ticket_booking.show.entity.ShowStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ShowRepository extends JpaRepository<Show, Long> {

    @EntityGraph(attributePaths = "pricing")
    Optional<Show> findById(Long id);

    @Query("""
            select (count(s) > 0) from Show s
            where s.screenId = :screenId and s.status = :status and s.id <> :excludeId
              and s.startTime < :endTime and s.endTime > :startTime
            """)
    boolean overlaps(@Param("screenId") Long screenId, @Param("status") ShowStatus status,
                     @Param("excludeId") Long excludeId,
                     @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("""
            select s from Show s
            where s.cityId = :cityId and s.status = :status
              and (:movieId is null or s.movieId = :movieId)
              and s.startTime >= :from and s.startTime < :to
            order by s.theatreId asc, s.startTime asc
            """)
    List<Show> search(@Param("cityId") Long cityId, @Param("movieId") Long movieId,
                      @Param("status") ShowStatus status,
                      @Param("from") Instant from, @Param("to") Instant to);
}
