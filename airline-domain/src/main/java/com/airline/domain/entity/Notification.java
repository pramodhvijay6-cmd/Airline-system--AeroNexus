package com.airline.domain.entity;

import com.airline.common.model.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Builder.Default
    @Column(length = 20)
    private String channel = "EMAIL";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
}
