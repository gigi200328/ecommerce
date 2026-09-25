package com.ojt.ecommerce.backofficeinventory.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponseDto {

    private Long categoryId;
    private String categoryName;
    private String description;
    private Long parentId;
    private String parentName;

    // Parent-Child Hierarchy အတွက်
    private List<CategoryResponseDto> children;

    private Long createdByUserId;
    private LocalDateTime createdAt;
    private Long modifiedByUserId;
    private LocalDateTime modifiedAt;
}