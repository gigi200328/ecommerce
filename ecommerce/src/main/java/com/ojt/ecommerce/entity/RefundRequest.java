package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "refund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id", nullable = false)
    private Long refundId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", referencedColumnName = "order_item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refund_requests_order_item"))
    private OrderItem orderItem;

    @Column(name = "refund_quantity", nullable = false)
    private Integer refundQuantity;

    @Column(name = "request_reason", nullable = false, columnDefinition = "ENUM('DAMAGED_ITEM', 'DEFECTIVE_PRODUCT', 'MISSING_PARTS', 'ORDERED_BY_MISTAKE', 'WRONG_ITEM_SENT', 'OTHER')")
    private String requestReason;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "status", nullable = false, columnDefinition = "ENUM('PENDING', 'APPROVED', 'REJECTED')")
    private String status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "processed_by", referencedColumnName = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_refund_processed_by"))
    private User processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
}
