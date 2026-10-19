
package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.CategoryRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.CategoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.CategoryMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandCategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.specification.CategorySpecification;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BrandCategoryRepository brandCategoryRepository;
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    // Maximum allowed category hierarchy depth
    private static final int MAX_DEPTH = 3;

    // =========================================================
    // CREATE CATEGORY
    // =========================================================

    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto request) {

        User currentUser = getCurrentUser();

        Category parent = null;

        if (request.getParentId() != null) {

            parent = categoryRepository
                    .findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent category not found: " + request.getParentId()));

            // Validate the maximum category hierarchy depth
            int parentDepth = getCategoryDepth(parent);

            if (parentDepth >= MAX_DEPTH) {
                throw new IllegalArgumentException(
                        "Maximum category depth limit (" + MAX_DEPTH
                                + " levels) reached. Cannot add a subcategory beyond level "
                                + MAX_DEPTH + ".");
            }
        }

        // Validate duplicate category names under the same parent
        validateDuplicateCategoryName(
                request.getCategoryName(),
                request.getParentId(),
                null);

        LocalDateTime now = LocalDateTime.now();

        Category category = Category.builder()
                .categoryName(request.getCategoryName().trim())
                .description(request.getDescription())
                .parent(parent)
                .createdBy(currentUser)
                .createdAt(now)
                .modifiedBy(currentUser)
                .modifiedAt(now)
                .build();

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponseDto(savedCategory);
    }

    // =========================================================
    // GET ALL CATEGORIES
    // =========================================================

    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> getAllCategories(Pageable pageable) {

        return categoryRepository
                .findAll(pageable)
                .map(categoryMapper::toResponseDto);
    }

    // =========================================================
    // GET CATEGORY TREE
    // =========================================================

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoryTree() {

        List<Category> allCategories = categoryRepository.findAll();

        return categoryMapper.toTreeDtoList(allCategories);
    }

    // =========================================================
    // GET CATEGORY BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + id));

        return categoryMapper.toResponseDto(category);
    }

    // =========================================================
    // UPDATE CATEGORY
    // =========================================================

    @Transactional
    public CategoryResponseDto updateCategory(
            Long id,
            CategoryRequestDto request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + id));

        User currentUser = getCurrentUser();

        // Validate the parent category
        if (request.getParentId() != null) {

            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException(
                        "A category cannot be its own parent.");
            }

            Category parent = categoryRepository
                    .findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent category not found: "
                                    + request.getParentId()));

            // Prevent circular category relationships
            if (isDescendant(category, parent)) {
                throw new IllegalArgumentException(
                        "Cannot set a child category as its parent.");
            }

            // Validate the maximum hierarchy depth after moving the category
            int newParentDepth = getCategoryDepth(parent);
            int currentSubtreeDepth = getMaxSubtreeDepth(category);

            if (newParentDepth + currentSubtreeDepth > MAX_DEPTH) {
                throw new IllegalArgumentException(
                        "Moving this category will exceed the maximum allowed depth limit of "
                                + MAX_DEPTH + " levels.");
            }

            category.setParent(parent);

        } else {
            category.setParent(null);
        }

        // Validate duplicate names while excluding the current category
        validateDuplicateCategoryName(
                request.getCategoryName(),
                request.getParentId(),
                id);

        // Update category fields
        category.setCategoryName(request.getCategoryName().trim());
        category.setDescription(request.getDescription());
        category.setModifiedBy(currentUser);
        category.setModifiedAt(LocalDateTime.now());

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponseDto(savedCategory);
    }

    // =========================================================
    // DELETE CATEGORY
    // =========================================================

    @Transactional
    public void deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + id));

        // Prevent deletion when child categories exist
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete a category containing child categories. "
                            + "Delete subcategories first.");
        }

        // Prevent deletion when the category is associated with brands
        if (brandCategoryRepository.existsByCategoryCategoryId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete this category because it is associated "
                            + "with one or more brands.");
        }

        categoryRepository.delete(category);
    }

    // =========================================================
    // VALIDATE DUPLICATE CATEGORY NAMES
    // =========================================================

    private void validateDuplicateCategoryName(
            String categoryName,
            Long parentId,
            Long excludeCategoryId) {

        String normalizedName = categoryName.trim();

        boolean exists;

        if (parentId == null) {

            // Check duplicate root category names
            if (excludeCategoryId == null) {
                exists = categoryRepository
                        .existsByCategoryNameIgnoreCaseAndParentIsNull(
                                normalizedName);
            } else {
                exists = categoryRepository
                        .existsByCategoryNameIgnoreCaseAndParentIsNullAndCategoryIdNot(
                                normalizedName,
                                excludeCategoryId);
            }

        } else {

            // Check duplicate subcategory names under the same parent
            if (excludeCategoryId == null) {
                exists = categoryRepository
                        .existsByCategoryNameIgnoreCaseAndParent_CategoryId(
                                normalizedName,
                                parentId);
            } else {
                exists = categoryRepository
                        .existsByCategoryNameIgnoreCaseAndParent_CategoryIdAndCategoryIdNot(
                                normalizedName,
                                parentId,
                                excludeCategoryId);
            }
        }

        // Reject duplicate category names
        if (exists) {
            throw new IllegalArgumentException(
                    "Category name already exists under this parent: "
                            + normalizedName);
        }
    }

    // =========================================================
    // GET CURRENT AUTHENTICATED USER
    // =========================================================

    private User getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "User is not authenticated.");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found: " + email));
    }

    // =========================================================
    // CHECK CIRCULAR CATEGORY RELATIONSHIPS
    // =========================================================

    private boolean isDescendant(
            Category currentCategory,
            Category potentialParent) {

        Category parent = potentialParent.getParent();

        while (parent != null) {

            if (parent.getCategoryId()
                    .equals(currentCategory.getCategoryId())) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
    }

    // =========================================================
    // CALCULATE CATEGORY DEPTH
    // =========================================================

    /**
     * Calculates the category depth, where the root category is level 1.
     */
    private int getCategoryDepth(Category category) {

        int depth = 1;
        Category current = category;

        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }

        return depth;
    }

    /**
     * Calculates the maximum depth of a category and its descendants.
     */
    private int getMaxSubtreeDepth(Category category) {

        if (category.getChildren() == null
                || category.getChildren().isEmpty()) {
            return 1;
        }

        int maxChildDepth = 0;

        for (Category child : category.getChildren()) {
            maxChildDepth = Math.max(
                    maxChildDepth,
                    getMaxSubtreeDepth(child));
        }

        return 1 + maxChildDepth;
    }

    // =========================================================
    // SEARCH CATEGORIES
    // =========================================================

    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> searchCategories(
            String keyword,
            Pageable pageable) {

        Specification<Category> spec =
                CategorySpecification.search(keyword);

        return categoryRepository
                .findAll(spec, pageable)
                .map(categoryMapper::toResponseDto);
    }
}
