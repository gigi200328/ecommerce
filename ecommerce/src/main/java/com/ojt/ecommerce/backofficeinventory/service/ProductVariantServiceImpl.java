package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductVariant;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;

    @Override
    public ProductVariant createProductVariant(ProductVariant productVariant) {
        if (productVariant.getProduct() == null || productVariant.getProduct().getProductId() == null) {
            throw new RuntimeException("Product is required for creating a variant.");
        }

        Long productId = productVariant.getProduct().getProductId();

        // Check Product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Prevent duplicate SKU
        if (productVariantRepository.existsBySku(productVariant.getSku())) {
            throw new RuntimeException("A product variant with SKU '" + productVariant.getSku() + "' already exists.");
        }

        productVariant.setProduct(product);
        return productVariantRepository.save(productVariant);
    }

    @Override
    public List<ProductVariant> getAllProductVariants() {
        return productVariantRepository.findAll();
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
}
