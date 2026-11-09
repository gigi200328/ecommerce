package com.ojt.ecommerce.storefront.exception;

import lombok.Getter;

@Getter
public class ActiveHoldConflictException extends ConflictException {
    private final Long existingOrderId;
    private final String existingOrderNo;
    private final String expiresAt;

    public ActiveHoldConflictException(Long existingOrderId, String existingOrderNo, String expiresAt, String message) {
        super(message);
        this.existingOrderId = existingOrderId;
        this.existingOrderNo = existingOrderNo;
        this.expiresAt = expiresAt;
    }
}
