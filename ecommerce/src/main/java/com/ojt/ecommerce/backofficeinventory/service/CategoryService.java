package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Category Level ကို အများဆုံး ၃ ဆင့်အထိပဲ ခွင့်ပြုရန် Constant သတ်မှတ်ခြင်း
    private static final int MAX_DEPTH = 3;

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto request) {

        User currentUser = getCurrentUser();

        Category parent = null;

        if (request.getParentId() != null) {

            parent = categoryRepository
                    .findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent Category not found: " + request.getParentId()
                    ));

            // -----------------------------------------------------
            // Category Depth Validation (၃ ဆင့်ထက် ကျော်မကျော် စစ်ဆေးခြင်း)
            // -----------------------------------------------------
            int parentDepth = getCategoryDepth(parent);
            if (parentDepth >= MAX_DEPTH) {
                throw new IllegalArgumentException(
                        "Maximum category depth limit (" + MAX_DEPTH + " levels) reached. " +
                        "Cannot add subcategory beyond Level " + MAX_DEPTH + "."
                );
            }
        }

        LocalDateTime now = LocalDateTime.now();

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
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
    // READ ALL
    // =========================================================

    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> getAllCategories(Pageable pageable) {

        return categoryRepository
                .findAll(pageable)
                .map(categoryMapper::toResponseDto);
    }

    // =========================================================
    // READ TREE
    // =========================================================

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoryTree() {

        List<Category> allCategories = categoryRepository.findAll();

        return categoryMapper.toTreeDtoList(allCategories);
    }

    // =========================================================
    // READ BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + id
                ));

        return categoryMapper.toResponseDto(category);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + id
                ));

        User currentUser = getCurrentUser();

        // -----------------------------------------------------
        // Parent validation
        // -----------------------------------------------------

        if (request.getParentId() != null) {

            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException(
                        "A category cannot be its own parent."
                );
            }

            Category parent = categoryRepository
                    .findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent Category not found: " + request.getParentId()
                    ));

            if (isDescendant(category, parent)) {
                throw new IllegalArgumentException(
                        "Cannot set a child category as its parent."
                );
            }

            // -----------------------------------------------------
            // Category Depth Validation (Update အတွက်ပါ စစ်ဆေးခြင်း)
            // -----------------------------------------------------
            int newParentDepth = getCategoryDepth(parent);
            int currentSubtreeDepth = getMaxSubtreeDepth(category);

            if (newParentDepth + currentSubtreeDepth > MAX_DEPTH) {
                throw new IllegalArgumentException(
                        "Moving this category will exceed the maximum allowed depth limit of " + MAX_DEPTH + " levels."
                );
            }

            category.setParent(parent);

        } else {

            category.setParent(null);
        }

        // -----------------------------------------------------
        // Update fields
        // -----------------------------------------------------

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setModifiedBy(currentUser);
        category.setModifiedAt(LocalDateTime.now());

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponseDto(savedCategory);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category not found: " + id
                ));

        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete category containing child categories. Delete sub-categories first."
            );
        }

        if (brandCategoryRepository.existsByCategoryCategoryId(id)) {
            throw new IllegalArgumentException(
                    "Cannot delete category because it is associated with one or more brands."
            );
        }

        categoryRepository.delete(category);
    }

    // =========================================================
    // GET CURRENT LOGGED-IN USER
    // =========================================================

    private User getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found: " + email
                ));
    }

    // =========================================================
    // CHECK CIRCULAR RELATIONSHIP
    // =========================================================

    private boolean isDescendant(Category currentCategory, Category potentialParent) {

        Category parent = potentialParent.getParent();

        while (parent != null) {

            if (parent.getCategoryId().equals(currentCategory.getCategoryId())) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
    }

    // =========================================================
    // HELPER METHODS FOR DEPTH CALCULATION
    // =========================================================

    /**
     * ရွေးချယ်လိုက်သော Parent ရဲ့ လက်ရှိ Depth (အဆင့်) ကို တွက်ပေးသည် (Root = Level 1)
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
     * Update လုပ်သည့်အခါ အဆိုပါ Category ရဲ့ အောက်မှာ ရှိနေသည့် Child Subcategory များ၏ အနက်ဆုံး Depth ကို တွက်ပေးသည်
     */
    private int getMaxSubtreeDepth(Category category) {
        if (category.getChildren() == null || category.getChildren().isEmpty()) {
            return 1;
        }
        int maxChildDepth = 0;
        for (Category child : category.getChildren()) {
            maxChildDepth = Math.max(maxChildDepth, getMaxSubtreeDepth(child));
        }
        return 1 + maxChildDepth;
    }
}