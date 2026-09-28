package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.ProductVariantStatus;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    // Check duplicate SKU
    boolean existsBySku(String sku);

    // Check duplicate SKU excluding current variant ID for update
    boolean existsBySkuAndVariantIdNot(String sku, Long variantId);

    // Find variant by SKU
    Optional<ProductVariant> findBySku(String sku);

    // Get all variants belonging to a specific product
    List<ProductVariant> findByProduct_ProductId(Long productId);

    // Find variants by status
    List<ProductVariant> findByStatus(ProductVariantStatus status);
}
