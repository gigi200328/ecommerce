package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id", nullable = false)
    private Long txnId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", referencedColumnName = "variant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_txn_variant"))
    private ProductVariant variant;

    @Column(name = "txn_type", nullable = false, columnDefinition = "ENUM('RESTOCK', 'SALE', 'RETURN', 'WRITE_OFF', 'ADJUSTMENT')")
    private String txnType;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "before_qty", nullable = false)
    private Integer beforeQty;

    @Column(name = "after_qty", nullable = false)
    private Integer afterQty;

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "created_by", referencedColumnName = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_inventory_txn_created_by"))
    private User createdBy;
}
