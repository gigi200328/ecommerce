package com.ojt.ecommerce.storefront.order.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class OrderItemSummary {
    private Long orderItemId;
    private String productName;
    private Integer qty;
}