package com.ojt.ecommerce.backofficeinventory.mapper;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.InventoryTransactionResponseDto;
import com.ojt.ecommerce.entity.InventoryTransaction;

@Component
public class InventoryTransactionMapper {

    public InventoryTransactionResponseDto toResponseDto(InventoryTransaction txn) {
        if (txn == null) {
            return null;
        }

        Long variantId = null;
        String sku = null;
        String productName = null;

        if (txn.getVariant() != null) {
            variantId = txn.getVariant().getVariantId();
            sku = txn.getVariant().getSku();
            if (txn.getVariant().getProduct() != null) {
                productName = txn.getVariant().getProduct().getProductName();
            }
        }

        String createdByName = null;
        if (txn.getCreatedBy() != null) {
            createdByName = txn.getCreatedBy().getUserName();
        }

        return InventoryTransactionResponseDto.builder()
                .txnId(txn.getTxnId())
                .variantId(variantId)
                .sku(sku)
                .productName(productName)
                .txnType(txn.getTxnType())
                .qty(txn.getQty())
                .beforeQty(txn.getBeforeQty())
                .afterQty(txn.getAfterQty())
                .referenceType(txn.getReferenceType())
                .referenceId(txn.getReferenceId())
                .remark(txn.getRemark())
                .createdByName(createdByName)
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
