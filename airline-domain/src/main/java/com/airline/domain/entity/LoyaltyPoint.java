package com.airline.domain.entity;

import com.airline.common.model.LoyaltyTier;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "loyalty_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "current_balance")
    @Builder.Default
    private int currentBalance = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private LoyaltyTier tier = LoyaltyTier.BRONZE;
}
