package com.airline.domain.repository;

import com.airline.domain.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    Optional<Ticket> findByPassengerId(Long passengerId);
}
