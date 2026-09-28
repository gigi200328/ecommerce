package com.ojt.ecommerce.storefront.order.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class OrderStatusHistoryDto {
    private String oldStatus;
    private String newStatus;
    private String remark;
    private String changedAt;
}