package com.airline.domain.entity;

import com.airline.common.model.BaggageStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "baggage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Baggage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @Column(name = "bag_tag", unique = true, nullable = false, length = 20)
    private String bagTag;

    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private BaggageStatus status = BaggageStatus.CHECKED_IN;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
