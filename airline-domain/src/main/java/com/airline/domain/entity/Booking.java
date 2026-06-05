package com.airline.domain.entity;

import com.airline.common.model.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "booking_reference", unique = true, nullable = false, length = 10)
    private String bookingReference;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "booking_status", nullable = false, length = 20)
    private BookingStatus bookingStatus = BookingStatus.PENDING;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "booking_time", updatable = false)
    private LocalDateTime bookingTime;

    @PrePersist
    protected void onCreate() {
        bookingTime = LocalDateTime.now();
    }
}
