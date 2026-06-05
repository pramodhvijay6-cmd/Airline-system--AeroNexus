package com.airline.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aircraft")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "tail_number", nullable = false, unique = true, length = 20)
    private String tailNumber;

    @Column(nullable = false)
    private int capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    private Airline airline;

    @Builder.Default
    @Column(length = 20)
    private String status = "ACTIVE";
}
