package com.airline.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "routes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_route", columnNames = {"origin", "destination"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String origin;

    @Column(nullable = false, length = 50)
    private String destination;

    @Column(name = "distance_miles", nullable = false)
    private int distanceMiles;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;
}
