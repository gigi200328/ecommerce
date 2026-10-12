package com.ojt.ecommerce.storefront.refund.dto;
import lombok.Builder;
import lombok.Data;
@Data @Builder public class RefundEligibilityDto {
    private Long orderItemId;
    private Integer orderedQuantity;
    private Integer alreadyReservedRefundQuantity;
    private Integer remainingRefundableQuantity;
    private Boolean canRequestRefund;
}