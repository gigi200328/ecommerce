package com.ojt.ecommerce.storefront.refund.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class RefundResponseDto {
    private Long refundId;
    private Long orderItemId;
    private String productName;
    private String variantAttributes;
    private Integer orderedQuantity;
    private Integer requestedRefundQuantity;
    private Integer remainingRefundableQuantity;
    private String requestReason;
    private BigDecimal refundAmount;
    private String status;
    private String requestedAt;
}