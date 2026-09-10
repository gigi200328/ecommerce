package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "payment_callbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "callback_id", nullable = false)
    private Long callbackId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", referencedColumnName = "payment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_callbacks_payment"))
    private Payment payment;

    @Column(name = "external_event_id", nullable = false, length = 150)
    private String externalEventId;

    @Column(name = "transaction_ref", length = 100)
    private String transactionRef;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    @Column(name = "processing_status", nullable = false, columnDefinition = "ENUM('RECEIVED', 'PROCESSED', 'REJECTED')")
    private String processingStatus;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
