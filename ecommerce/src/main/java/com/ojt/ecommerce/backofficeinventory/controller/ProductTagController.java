package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductTagRepository;
import com.ojt.ecommerce.backofficeinventory.repository.TagRepository;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductTag;
import com.ojt.ecommerce.entity.Tag;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/product-tags")
@RequiredArgsConstructor
public class ProductTagController {

    private final ProductTagRepository productTagRepository;
    private final ProductRepository productRepository;
    private final TagRepository tagRepository;

    @Operation(summary = "Get tags for a specific product")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Map<String, Object>>> getTagsByProductId(@PathVariable Long productId) {
        List<ProductTag> productTags = productTagRepository.findByProductProductId(productId);
        List<Map<String, Object>> result = productTags.stream()
                .map(pt -> Map.<String, Object>of(
                        "tagId", pt.getTag().getTagId(),
                        "tagName", pt.getTag().getTagName()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Assign tags to a product (replaces or appends)")
    @PostMapping("/product/{productId}")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> assignTagsToProduct(
            @PathVariable Long productId,
            @RequestBody List<Long> tagIds) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        // Clear existing product tags
        productTagRepository.deleteByProductProductId(productId);

        List<ProductTag> savedTags = new ArrayList<>();
        if (tagIds != null && !tagIds.isEmpty()) {
            for (Long tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId).orElse(null);
                if (tag != null) {
                    ProductTag pt = ProductTag.builder()
                            .product(product)
                            .tag(tag)
                            .build();
                    savedTags.add(productTagRepository.save(pt));
                }
            }
        }

        List<Map<String, Object>> result = savedTags.stream()
                .map(pt -> Map.<String, Object>of(
                        "tagId", pt.getTag().getTagId(),
                        "tagName", pt.getTag().getTagName()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Remove a tag from a product")
    @DeleteMapping("/product/{productId}/tag/{tagId}")
    @Transactional
    public ResponseEntity<Void> removeTagFromProduct(
            @PathVariable Long productId,
            @PathVariable Long tagId) {
        productTagRepository.deleteByProductProductIdAndTagTagId(productId, tagId);
        return ResponseEntity.noContent().build();
    }
}
