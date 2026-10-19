package com.ojt.ecommerce.storefront.payment.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain Event representing the successful payment of an order.
 * Dispatched internally within G5 after payment callback succeeds.
 */
public record OrderPaidEvent(
    Long orderId,
    String orderNo,
    String transactionRef,
    BigDecimal amount,
    LocalDateTime paidAt
) {}
