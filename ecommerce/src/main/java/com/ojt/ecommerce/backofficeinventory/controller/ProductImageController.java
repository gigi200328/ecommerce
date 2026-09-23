package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.ProductImageRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductImageResponseDto;
import com.ojt.ecommerce.backofficeinventory.service.ProductImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    /**
     * Upload an image for an existing product
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductImageResponseDto> uploadImage(@Valid @ModelAttribute ProductImageRequestDto request) {
        ProductImageResponseDto response = productImageService.uploadProductImage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all images for a specific product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductImageResponseDto>> getImagesByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(productImageService.getImagesByProductId(productId));
    }

    /**
     * Delete an image by imageId
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long imageId) {
        productImageService.deleteProductImage(imageId);
        return ResponseEntity.noContent().build();
    }
}