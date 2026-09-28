package com.ojt.ecommerce.backofficeinventory.mapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.CategoryResponseDto;
import com.ojt.ecommerce.entity.Category;

@Component
public class CategoryMapper {

    // Single Entity -> DTO
    public CategoryResponseDto toResponseDto(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .parentId(category.getParent() != null
                        ? category.getParent().getCategoryId()
                        : null)
                .parentName(category.getParent() != null
                        ? category.getParent().getCategoryName()
                        : null)
                .createdByUserId(category.getCreatedBy() != null
                        ? category.getCreatedBy().getUserId()
                        : null)
                .createdAt(category.getCreatedAt())
                .modifiedByUserId(category.getModifiedBy() != null
                        ? category.getModifiedBy().getUserId()
                        : null)
                .modifiedAt(category.getModifiedAt())
                .build();
    }

    // Build Tree Efficiently
    public List<CategoryResponseDto> toTreeDtoList(List<Category> categories) {

        Map<Long, List<Category>> childrenByParentId = categories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(
                        c -> c.getParent().getCategoryId()
                ));

        return categories.stream()
                .filter(c -> c.getParent() == null)
                .map(root -> buildTree(root, childrenByParentId))
                .toList();
    }

    private CategoryResponseDto buildTree(
            Category category,
            Map<Long, List<Category>> childrenByParentId) {

        CategoryResponseDto dto = toResponseDto(category);

        List<CategoryResponseDto> children = childrenByParentId
                .getOrDefault(category.getCategoryId(), List.of())
                .stream()
                .map(child -> buildTree(child, childrenByParentId))
                .toList();

        dto.setChildren(children);

        return dto;
    }
}