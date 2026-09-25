package com.ojt.ecommerce.storefront.catalog.controller;

import com.ojt.ecommerce.storefront.catalog.dto.*;
import com.ojt.ecommerce.storefront.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/tags")
    public ResponseEntity<List<String>> getAllTags() {
        return ResponseEntity.ok(catalogService.getAllTags());
    }

    @GetMapping("/categories/tree")
    public ResponseEntity<List<CategoryNodeResponse>> getCategoryTree() {
        return ResponseEntity.ok(catalogService.getCategoryTree());
    }

    @GetMapping("/brands")
    public ResponseEntity<List<BrandResponse>> getActiveBrands() {
        return ResponseEntity.ok(catalogService.getActiveBrands());
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductListResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String availability,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.getProducts(categoryId, brandId, search, minPrice, maxPrice, availability, tags, sort, page, size));
    }
    
    @GetMapping("/products/best-sellers")
    public ResponseEntity<PageResponse<ProductListResponse>> getBestSellers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.getBestSellers(page, size));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductDetail(@PathVariable Long productId) {
        return ResponseEntity.ok(catalogService.getProductDetail(productId));
    }
    
    @GetMapping("/products/{productId}/related")
    public ResponseEntity<List<ProductListResponse>> getRelatedProducts(@PathVariable Long productId) {
        return ResponseEntity.ok(catalogService.getRelatedProducts(productId));
    }
}
