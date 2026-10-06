package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;

import com.ojt.ecommerce.entity.ProductVariant;

public interface ProductVariantService {

    ProductVariant createProductVariant(ProductVariant productVariant);

    List<ProductVariant> getAllProductVariants();

    ProductVariant getProductVariantById(Long id);

    List<ProductVariant> getVariantsByProductId(Long productId);

    ProductVariant updateProductVariant(Long id, ProductVariant productVariant);

    void deleteProductVariant(Long id);

    String generateSku(Long productId, List<Long> optionIds);
}
