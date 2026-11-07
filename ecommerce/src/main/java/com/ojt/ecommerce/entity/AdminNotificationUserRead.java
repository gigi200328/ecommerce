package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notification_user_reads", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notif_user", columnNames = {"notification_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminNotificationUserRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();
}
