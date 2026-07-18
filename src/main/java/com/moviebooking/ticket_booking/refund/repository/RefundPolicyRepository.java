package com.moviebooking.ticket_booking.refund.repository;

import com.moviebooking.ticket_booking.refund.entity.RefundPolicy;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    boolean existsByNameIgnoreCase(String name);

    /** Rules are eagerly fetched so response mapping can happen outside the transaction. */
    @EntityGraph(attributePaths = "rules")
    List<RefundPolicy> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "rules")
    Optional<RefundPolicy> findById(Long id);
}
