package com.ojt.ecommerce.storefront.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSnapshot {
    private Long orderItemId;
    private String productName;
    private String variantAttributes;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal costPrice;
    private BigDecimal subtotal;
}
