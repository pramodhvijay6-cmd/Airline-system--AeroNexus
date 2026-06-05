package com.airline.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "passenger_id", nullable = false, unique = true)
    private Passenger passenger;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 20)
    private String ticketNumber;

    @Builder.Default
    @Column(name = "ticket_status", nullable = false, length = 20)
    private String ticketStatus = "ACTIVE";

    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
    }
}
