package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Delete Safety စစ်ရန်အတွက် Child Category ရှိမရှိ စစ်ဆေးခြင်း
    boolean existsByParentCategoryId(Long parentId);
}