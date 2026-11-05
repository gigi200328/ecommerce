package com.ojt.ecommerce.storefront.reservation.entity;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inventory_reservations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_order_variant", columnNames = {"order_id", "variant_id"})
        },
        indexes = {
                @Index(name = "idx_inv_res_status_expires", columnList = "status, expires_at"),
                @Index(name = "idx_inv_res_variant_status", columnList = "variant_id, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryReservation {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CONSUMED = "CONSUMED";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_res_order"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", referencedColumnName = "variant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inventory_res_variant"))
    private ProductVariant variant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "status", nullable = false, columnDefinition = "ENUM('RESERVED','CONSUMED','RELEASED','EXPIRED')")
    private String status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
