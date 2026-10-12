package com.ojt.ecommerce.storefront.refund.dto;
import lombok.Data;
@Data public class RefundRequestDto {
    private Long orderItemId;
    private Integer refundQuantity;
    private String requestReason;
}