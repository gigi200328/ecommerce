package com.ojt.ecommerce.storefront.payment.dto;
import lombok.Data;
@Data public class MockCallbackRequest {
    private String externalEventId;
    private String transactionRef;
    private String status;
}