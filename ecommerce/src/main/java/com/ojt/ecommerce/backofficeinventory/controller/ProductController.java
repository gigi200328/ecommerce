package com.ojt.ecommerce.backofficeinventory.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ojt.ecommerce.backofficeinventory.dto.ProductRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductResponseDto;
import com.ojt.ecommerce.backofficeinventory.service.ProductService;
import com.ojt.ecommerce.enums.ProductStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	// =========================================================
	// 1. CREATE
	// =========================================================
	@PostMapping
	public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto request) {

		return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
	}

	// =========================================================
	// 2. SEARCH / FILTER
	// =========================================================
	@GetMapping("/search")
	public ResponseEntity<Page<ProductResponseDto>> searchProducts(

			@RequestParam(required = false) String keyword,

			@RequestParam(required = false) Long categoryId,

			@RequestParam(required = false) Long brandId,

			@RequestParam(required = false) ProductStatus status,

			@RequestParam(required = false) BigDecimal minPrice,

			@RequestParam(required = false) BigDecimal maxPrice,

			@PageableDefault(size = 20, sort = "productName") Pageable pageable) {

		return ResponseEntity
				.ok(productService.searchProducts(keyword, categoryId, brandId, status, minPrice, maxPrice, pageable));
	}

	// =========================================================
	// 3. READ ALL (PAGINATED)
	// =========================================================
	@GetMapping
	public ResponseEntity<Page<ProductResponseDto>> getAllProducts(
			@PageableDefault(size = 20, sort = "productName") Pageable pageable) {

		return ResponseEntity.ok(productService.getAllProducts(pageable));
	}

	// =========================================================
	// 4. READ BY ID
	// =========================================================
	@GetMapping("/{id}")
	public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {

		return ResponseEntity.ok(productService.getProductById(id));
	}

	// =========================================================
	// 5. READ BY CATEGORY
	// =========================================================
	@GetMapping("/category/{categoryId}")
	public ResponseEntity<Page<ProductResponseDto>> getProductsByCategoryId(@PathVariable Long categoryId,
			@PageableDefault(size = 20, sort = "productName") Pageable pageable) {

		return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId, pageable));
	}

	// =========================================================
	// 6. READ BY BRAND
	// =========================================================
	@GetMapping("/brand/{brandId}")
	public ResponseEntity<Page<ProductResponseDto>> getProductsByBrandId(@PathVariable Long brandId,
			@PageableDefault(size = 20, sort = "productName") Pageable pageable) {

		return ResponseEntity.ok(productService.getProductsByBrandId(brandId, pageable));
	}

	// =========================================================
	// 7. READ BY STATUS
	// =========================================================
	@GetMapping("/status/{status}")
	public ResponseEntity<Page<ProductResponseDto>> getProductsByStatus(@PathVariable ProductStatus status,
			@PageableDefault(size = 20, sort = "productName") Pageable pageable) {

		return ResponseEntity.ok(productService.getProductsByStatus(status, pageable));
	}

	// =========================================================
	// 8. UPDATE
	// =========================================================
	@PutMapping("/{id}")
	public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Long id,
			@Valid @RequestBody ProductRequestDto request) {

		return ResponseEntity.ok(productService.updateProduct(id, request));
	}

	// =========================================================
	// 9. DELETE
	// =========================================================
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {

		productService.deleteProduct(id);

		return ResponseEntity.noContent().build();
	}
}