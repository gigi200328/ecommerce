package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.ProductVariantCreateDto;
import com.ojt.ecommerce.backofficeinventory.repository.InventoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.InventoryTransactionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariantOptionValueRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.InventoryTransaction;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.ProductVariantStatus;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.entity.VariantOptionValue;
import com.ojt.ecommerce.entity.VariationOption;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final VariationOptionRepository variationOptionRepository;
    private final VariantOptionValueRepository variantOptionValueRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;

    @Override
    public ProductVariant createProductVariant(ProductVariant productVariant) {
        if (productVariant.getProduct() == null || productVariant.getProduct().getProductId() == null) {
            throw new RuntimeException("Product is required for creating a variant.");
        }

        Long productId = productVariant.getProduct().getProductId();

        // Check Product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Auto-generate SKU if not provided
        if (productVariant.getSku() == null || productVariant.getSku().trim().isEmpty()) {
            productVariant.setSku(generateSku(productId, null));
        } else {
            // Prevent duplicate SKU
            if (productVariantRepository.existsBySku(productVariant.getSku())) {
                throw new RuntimeException("A product variant with SKU '" + productVariant.getSku() + "' already exists.");
            }
        }

        productVariant.setProduct(product);
        ProductVariant savedVariant = productVariantRepository.save(productVariant);

        // Ensure inventory record exists for this variant
        if (!inventoryRepository.existsByVariant_VariantId(savedVariant.getVariantId())) {
            User currentUser = getCurrentUser();
            LocalDateTime now = LocalDateTime.now();
            Inventory inventory = Inventory.builder()
                    .variant(savedVariant)
                    .quantity(0)
                    .reservedQuantity(0)
                    .reorderLevel(10)
                    .status("OUT_OF_STOCK")
                    .createdBy(currentUser)
                    .createdAt(now)
                    .modifiedBy(currentUser)
                    .modifiedAt(now)
                    .build();
            inventoryRepository.save(inventory);
        }

        return savedVariant;
    }

    @Override
    @Transactional
    public ProductVariant createProductVariantWithOptions(ProductVariantCreateDto dto) {
        if (dto.getProductId() == null) {
            throw new RuntimeException("Product ID is required for creating a variant.");
        }

        Long productId = dto.getProductId();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        String sku = dto.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            sku = generateSku(productId, dto.getOptionIds());
        } else {
            if (productVariantRepository.existsBySku(sku.trim())) {
                throw new RuntimeException("A product variant with SKU '" + sku + "' already exists.");
            }
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku.trim())
                .sellingPrice(dto.getSellingPrice())
                .costPrice(dto.getCostPrice())
                .status(dto.getStatus() != null ? dto.getStatus() : ProductVariantStatus.ACTIVE)
                .build();

        ProductVariant savedVariant = productVariantRepository.save(variant);

        // Map options atomically
        if (dto.getOptionIds() != null && !dto.getOptionIds().isEmpty()) {
            for (Long optionId : dto.getOptionIds()) {
                if (optionId != null) {
                    VariationOption option = variationOptionRepository.findById(optionId).orElse(null);
                    if (option != null && !variantOptionValueRepository.existsByVariant_VariantIdAndOption_OptionId(savedVariant.getVariantId(), optionId)) {
                        VariantOptionValue mapping = VariantOptionValue.builder()
                                .variant(savedVariant)
                                .option(option)
                                .build();
                        variantOptionValueRepository.save(mapping);
                    }
                }
            }
        }

        // Initialize Inventory atomically
        int initialQty = dto.getInitialStock() != null ? Math.max(0, dto.getInitialStock()) : 0;
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        String invStatus = initialQty == 0 ? "OUT_OF_STOCK" : (initialQty <= 10 ? "LOW_STOCK" : "NORMAL");

        Inventory inventory = Inventory.builder()
                .variant(savedVariant)
                .quantity(initialQty)
                .reservedQuantity(0)
                .reorderLevel(10)
                .status(invStatus)
                .createdBy(currentUser)
                .createdAt(now)
                .modifiedBy(currentUser)
                .modifiedAt(now)
                .build();
        inventoryRepository.save(inventory);

        if (initialQty > 0) {
            InventoryTransaction txn = InventoryTransaction.builder()
                    .variant(savedVariant)
                    .txnType("RESTOCK")
                    .qty(initialQty)
                    .beforeQty(0)
                    .afterQty(initialQty)
                    .referenceType("INITIAL_SETUP")
                    .remark("Initial stock setup upon variant creation")
                    .createdBy(currentUser)
                    .createdAt(now)
                    .build();
            inventoryTransactionRepository.save(txn);
        }

        return savedVariant;
    }

    @Override
    public List<ProductVariant> getAllProductVariants() {
        return productVariantRepository.findAll(Sort.by(Sort.Direction.DESC, "variantId"));
    }

    @Override
    public ProductVariant getProductVariantById(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + id));
    }

    @Override
    public List<ProductVariant> getVariantsByProductId(Long productId) {
        // Check Product exists
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found with id: " + productId);
        }

        return productVariantRepository.findByProduct_ProductId(productId);
    }

    @Override
    public ProductVariant updateProductVariant(Long id, ProductVariant productVariant) {
        ProductVariant existingVariant = getProductVariantById(id);

        // Prevent duplicate SKU
        if (productVariant.getSku() != null && !existingVariant.getSku().equalsIgnoreCase(productVariant.getSku())) {
            if (productVariantRepository.existsBySkuAndVariantIdNot(productVariant.getSku(), id)) {
                throw new RuntimeException("A product variant with SKU '" + productVariant.getSku() + "' already exists.");
            }
            existingVariant.setSku(productVariant.getSku());
        }

        // Update Product reference if provided
        if (productVariant.getProduct() != null && productVariant.getProduct().getProductId() != null) {
            Long productId = productVariant.getProduct().getProductId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            existingVariant.setProduct(product);
        }

        if (productVariant.getSellingPrice() != null) {
            existingVariant.setSellingPrice(productVariant.getSellingPrice());
        }
        if (productVariant.getCostPrice() != null) {
            existingVariant.setCostPrice(productVariant.getCostPrice());
        }
        if (productVariant.getStatus() != null) {
            existingVariant.setStatus(productVariant.getStatus());
        }

        return productVariantRepository.save(existingVariant);
    }

    @Override
    public void deleteProductVariant(Long id) {
        ProductVariant variant = getProductVariantById(id);
        productVariantRepository.delete(variant);
    }

    @Override
    public String generateSku(Long productId, List<Long> optionIds) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Create base SKU from product name
        String baseName = product.getProductName()
                .trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (baseName.isEmpty()) {
            baseName = "PROD-" + productId;
        }

        StringBuilder skuBuilder = new StringBuilder(baseName);

        if (optionIds != null && !optionIds.isEmpty()) {
            List<VariationOption> options = variationOptionRepository.findAllById(optionIds);
            for (VariationOption option : options) {
                String optVal = option.getValue()
                        .trim()
                        .toUpperCase()
                        .replaceAll("[^A-Z0-9]+", "-")
                        .replaceAll("^-+|-+$", "");
                if (!optVal.isEmpty()) {
                    skuBuilder.append("-").append(optVal);
                }
            }
        }

        String candidateSku = skuBuilder.toString();
        String finalSku = candidateSku;
        int counter = 1;

        // Prevent collision with existing SKUs
        while (productVariantRepository.existsBySku(finalSku)) {
            finalSku = candidateSku + String.format("-%02d", counter++);
        }

        return finalSku;
    }

    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                return ((CustomUserDetails) authentication.getPrincipal()).getUser();
            }
        } catch (Exception ignored) {
        }
        return userRepository.findByEmail("admin@ecommerce.com").orElse(null);
    }
}
