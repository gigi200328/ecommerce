package com.ojt.ecommerce.storefront.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrderExpiryResponse {
    private Long orderId;
    private String orderNo;
    private String expiresAt;
    private Integer pendingCount;
}
