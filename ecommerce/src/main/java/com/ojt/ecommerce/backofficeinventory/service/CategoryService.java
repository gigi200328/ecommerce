package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.CategoryRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.CategoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.CategoryMapper;
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
    private final UserRepository userRepository;
    private final CategoryMapper categoryMapper;

    // 1. CREATE
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto request) {
    	
    	User user = userRepository.findById(request.getUserId())
    	        .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent Category not found: " + request.getParentId()));
        }

        LocalDateTime now = LocalDateTime.now();

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .parent(parent)
                .createdBy(user)
                .createdAt(now)
                .modifiedBy(user)
                .modifiedAt(now)
                .build();

        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    // 2. READ ALL (FLAT LIST WITH PAGINATION)
    @Transactional(readOnly = true)
    public Page<CategoryResponseDto> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(categoryMapper::toResponseDto);
    }

    // 3. READ NESTED TREE HIERARCHY
    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        return categoryMapper.toTreeDtoList(allCategories);
    }

    // 4. READ BY ID
    @Transactional(readOnly = true)
    public CategoryResponseDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        return categoryMapper.toResponseDto(category);
    }

    // 5. UPDATE
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));


        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent.");
            }

            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent Category not found: " + request.getParentId()));

            // Circular Dependency Validation Check (မိမိ၏ Child/Grandchild ကို Parent ပြန်မလုပ်နိုင်စေရန်)
            if (isDescendant(category, parent)) {
                throw new IllegalArgumentException("Cannot set a child category as its parent.");
            }

            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setModifiedBy(user);
        category.setModifiedAt(LocalDateTime.now());

        return categoryMapper.toResponseDto(categoryRepository.save(category));
    }

    // 6. DELETE
    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("Category not found: " + id);
        }

        // Child တွေကျန်နေပါက Delete လုပ်ခွင့်မပေးခြင်း
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete category containing child categories. Delete sub-categories first.");
        }

        categoryRepository.deleteById(id);
    }

    // Helper: Check Circular Relationship
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
}