
package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.CategoryRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.CategoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Create a new category
    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(
            @Valid @RequestBody CategoryRequestDto request) {

        CategoryResponseDto response =
                categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get all categories with pagination and sorting
    @GetMapping
    public ResponseEntity<Page<CategoryResponseDto>> getAllCategories(
            @PageableDefault(size = 20, sort = "categoryName")
            Pageable pageable) {

        return ResponseEntity.ok(
                categoryService.getAllCategories(pageable));
    }

    // Get the hierarchical category tree
    @GetMapping("/tree")
    public ResponseEntity<List<CategoryResponseDto>> getCategoryTree() {

        return ResponseEntity.ok(
                categoryService.getCategoryTree());
    }

    // Get a category by ID
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                categoryService.getCategoryById(id));
    }

    // Update an existing category
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDto request) {

        return ResponseEntity.ok(
                categoryService.updateCategory(id, request));
    }

    // Delete a category
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id) {

        categoryService.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }

    // Search categories by keyword with pagination
    @GetMapping("/search")
    public ResponseEntity<Page<CategoryResponseDto>> searchCategories(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        Page<CategoryResponseDto> response =
                categoryService.searchCategories(keyword, pageable);

        return ResponseEntity.ok(response);
    }
}
