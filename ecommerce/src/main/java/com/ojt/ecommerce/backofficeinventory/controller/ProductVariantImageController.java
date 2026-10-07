package com.ojt.ecommerce.backofficeinventory.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantImageRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.service.FileStorageService;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.ProductVariantImage;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.UploadType;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/product-variant-images")
@RequiredArgsConstructor
public class ProductVariantImageController {

    private final ProductVariantImageRepository productVariantImageRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload image specifically for a product variant")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadVariantImage(
            @RequestParam("variantId") Long variantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "userId", required = false, defaultValue = "1") Long userId) {

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found: " + variantId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String imageUrl = fileStorageService.store(file, UploadType.PRODUCT_IMAGE);
        LocalDateTime now = LocalDateTime.now();

        ProductVariantImage pvi = ProductVariantImage.builder()
                .productVariant(variant)
                .imageUrl(imageUrl)
                .isPrimary(isPrimary)
                .createdBy(user)
                .createdAt(now)
                .modifiedBy(user)
                .modifiedAt(now)
                .build();

        ProductVariantImage saved = productVariantImageRepository.save(pvi);

        Map<String, Object> response = Map.of(
                "productVariantImageId", saved.getProductVariantImageId(),
                "variantId", variant.getVariantId(),
                "imageUrl", saved.getImageUrl(),
                "isPrimary", saved.getIsPrimary(),
                "createdAt", saved.getCreatedAt().toString()
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get images for a product variant")
    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<Map<String, Object>>> getImagesByVariantId(@PathVariable Long variantId) {
        List<ProductVariantImage> images = productVariantImageRepository.findByProductVariantVariantId(variantId);
        List<Map<String, Object>> result = images.stream()
                .map(img -> Map.<String, Object>of(
                        "productVariantImageId", img.getProductVariantImageId(),
                        "imageUrl", img.getImageUrl(),
                        "isPrimary", img.getIsPrimary()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Delete variant image by ID")
    @DeleteMapping("/{imageId}")
    @Transactional
    public ResponseEntity<Void> deleteVariantImage(@PathVariable Long imageId) {
        productVariantImageRepository.deleteById(imageId);
        return ResponseEntity.noContent().build();
    }
}
