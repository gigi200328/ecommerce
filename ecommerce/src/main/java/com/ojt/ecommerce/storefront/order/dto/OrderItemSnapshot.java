package com.ojt.ecommerce.storefront.order.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class OrderItemSnapshot {
    private Long orderItemId;
    private String productName;
    private String variantAttributes;
    private Integer qty;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
}