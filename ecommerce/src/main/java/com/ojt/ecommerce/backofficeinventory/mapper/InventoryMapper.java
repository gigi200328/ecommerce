package com.ojt.ecommerce.backofficeinventory.mapper;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.InventoryResponseDto;
import com.ojt.ecommerce.entity.Inventory;

@Component
public class InventoryMapper {

    public InventoryResponseDto toResponseDto(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        Integer qty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        Integer reserved = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
        Integer available = Math.max(0, qty - reserved);

        Long variantId = null;
        String sku = null;
        String productName = null;
        java.math.BigDecimal sellingPrice = null;

        if (inventory.getVariant() != null) {
            variantId = inventory.getVariant().getVariantId();
            sku = inventory.getVariant().getSku();
            sellingPrice = inventory.getVariant().getSellingPrice();
            if (inventory.getVariant().getProduct() != null) {
                productName = inventory.getVariant().getProduct().getProductName();
            }
        }

        Boolean isPerishable = inventory.getIsPerishable();
        if (isPerishable == null && inventory.getVariant() != null && inventory.getVariant().getProduct() != null) {
            isPerishable = inventory.getVariant().getProduct().getIsPerishable();
        }
        if (isPerishable == null) {
            isPerishable = false;
        }

        java.time.LocalDate expiryDate = inventory.getExpiryDate();
        String batchNumber = inventory.getBatchNumber();
        Long daysUntilExpiry = null;
        String expiryStatus = "NON_PERISHABLE";

        if (Boolean.TRUE.equals(isPerishable) && expiryDate != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
            if (daysUntilExpiry < 0) {
                expiryStatus = "EXPIRED";
            } else if (daysUntilExpiry <= 30) {
                expiryStatus = "EXPIRING_SOON";
            } else {
                expiryStatus = "GOOD";
            }
        }

        return InventoryResponseDto.builder()
                .inventoryId(inventory.getInventoryId())
                .variantId(variantId)
                .sku(sku)
                .productName(productName)
                .sellingPrice(sellingPrice)
                .quantity(qty)
                .reservedQuantity(reserved)
                .availableQuantity(available)
                .reorderLevel(inventory.getReorderLevel())
                .status(inventory.getStatus())
                .isPerishable(isPerishable)
                .expiryDate(expiryDate)
                .batchNumber(batchNumber)
                .daysUntilExpiry(daysUntilExpiry)
                .expiryStatus(expiryStatus)
                .createdAt(inventory.getCreatedAt())
                .modifiedAt(inventory.getModifiedAt())
                .build();
    }
}
