package com.airline.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airlines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column(length = 100)
    private String country;
}
