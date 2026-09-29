package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentIsNull();
    List<Category> findByParentCategoryId(Long parentId);
    List<Category> findAll();
}