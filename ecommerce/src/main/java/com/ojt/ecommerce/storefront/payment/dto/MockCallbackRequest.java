package com.ojt.ecommerce.storefront.payment.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MockCallbackRequest {
    private String event;
    private String externalEventId;
    private String transactionRef;
    private String status;
    private String failureReason;
}