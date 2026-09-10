package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false, foreignKey = @ForeignKey(name = "order_status_history_ibfk_1"))
    private Order order;

    @Column(name = "old_status", columnDefinition = "ENUM('PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')")
    private String oldStatus;

    @Column(name = "new_status", nullable = false, columnDefinition = "ENUM('PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')")
    private String newStatus;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "changed_by", referencedColumnName = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_order_status_history_changed_by"))
    private User changedBy;
}
