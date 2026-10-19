
package com.ojt.ecommerce.backofficeinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Category;

@Repository
public interface CategoryRepository extends
        JpaRepository<Category, Long>,
        JpaSpecificationExecutor<Category> {

    // Check whether category has child categories
    boolean existsByParentCategoryId(Long parentId);

    // Check duplicate root category name (case-insensitive)
    boolean existsByCategoryNameIgnoreCaseAndParentIsNull(
            String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndParentIsNullAndCategoryIdNot(
            String categoryName, Long categoryId);

    // Check duplicate subcategory name under the same parent
    boolean existsByCategoryNameIgnoreCaseAndParent_CategoryId(
            String categoryName, Long parentId);

    boolean existsByCategoryNameIgnoreCaseAndParent_CategoryIdAndCategoryIdNot(
            String categoryName, Long parentId, Long categoryId);
}
