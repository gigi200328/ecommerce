package com.ojt.ecommerce.storefront.refund.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundEligibilityDto {
    private Long orderItemId;
    private Integer orderedQuantity;
    private Integer alreadyReservedRefundQuantity;
    private Integer remainingRefundableQuantity;

    @JsonProperty("canRequestRefund")
    private Boolean canRequestRefund;

    private String ineligibleReason;
    private Integer daysRemaining;

    private String refundStatus;

    public Boolean isCanRequestRefund() {
        return canRequestRefund;
    }

    public Boolean getCanRequestRefund() {
        return canRequestRefund;
    }
}
