package com.ojt.ecommerce.backofficeinventory.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.InventoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.repository.ProductImageRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantImageRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariantOptionValueRepository;
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.ProductImage;
import com.ojt.ecommerce.entity.ProductVariantImage;
import com.ojt.ecommerce.entity.VariantOptionValue;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InventoryMapper {

    private final VariantOptionValueRepository variantOptionValueRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantImageRepository productVariantImageRepository;

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
        String attributeSummary = null;
        String imageUrl = null;

        if (inventory.getVariant() != null) {
            variantId = inventory.getVariant().getVariantId();
            sku = inventory.getVariant().getSku();
            sellingPrice = inventory.getVariant().getSellingPrice();
            if (inventory.getVariant().getProduct() != null) {
                productName = inventory.getVariant().getProduct().getProductName();
            }

            if (variantId != null) {
                // 1. Resolve variant options summary
                List<VariantOptionValue> optionValues = variantOptionValueRepository.findByVariant_VariantId(variantId);
                if (optionValues != null && !optionValues.isEmpty()) {
                    attributeSummary = optionValues.stream()
                            .filter(v -> v.getOption() != null)
                            .map(v -> {
                                String attrName = (v.getOption().getVariation() != null && v.getOption().getVariation().getName() != null)
                                        ? v.getOption().getVariation().getName() : "";
                                String val = v.getOption().getValue() != null ? v.getOption().getValue() : "";
                                return !attrName.isEmpty() ? attrName + ": " + val : val;
                            })
                            .collect(Collectors.joining(" | "));
                }

                // 2. Resolve variant image if present
                List<ProductVariantImage> varImgs = productVariantImageRepository.findByProductVariantVariantId(variantId);
                if (varImgs != null && !varImgs.isEmpty() && varImgs.get(0).getImageUrl() != null && !varImgs.get(0).getImageUrl().isBlank()) {
                    imageUrl = varImgs.get(0).getImageUrl();
                }
            }

            // 3. Fallback to product primary image if no variant image
            if (imageUrl == null && inventory.getVariant().getProduct() != null) {
                Long prodId = inventory.getVariant().getProduct().getProductId();
                if (prodId != null) {
                    imageUrl = productImageRepository.findFirstByProductProductIdOrderByIsPrimaryDescImageIdAsc(prodId)
                            .map(ProductImage::getImageUrl)
                            .orElse(null);
                }
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
                .attributeSummary(attributeSummary)
                .imageUrl(imageUrl)
                .createdAt(inventory.getCreatedAt())
                .modifiedAt(inventory.getModifiedAt())
                .build();
    }
}
