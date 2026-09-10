package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payments_order"))
    private Order order;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "payment_token", length = 255)
    private String paymentToken;

    @Column(name = "payment_method", nullable = false, columnDefinition = "ENUM('MINI_BANKING', 'BANK_TRANSFER', 'CARD', 'KBZPAY', 'WAVEPAY')")
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false, columnDefinition = "ENUM('PENDING', 'SUCCESS', 'FAILED', 'PARTIALLY_REFUNDED', 'REFUNDED')")
    private String paymentStatus;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
