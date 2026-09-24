package com.ojt.ecommerce.storefront.catalog.repository;

import com.ojt.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface G5ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR p.category.categoryId = :categoryId) " +
           "AND (:brandId IS NULL OR p.brand.brandId = :brandId) " +
           "AND (:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("categoryId") Long categoryId, 
                                 @Param("brandId") Long brandId, 
                                 @Param("search") String search, 
                                 Pageable pageable);
                                 
    @Query("SELECT p FROM Product p JOIN ProductVariant v ON v.product = p JOIN OrderItem oi ON oi.variant = v JOIN oi.order o WHERE o.paymentStatus = 'SUCCESS' AND p.status = 'ACTIVE' GROUP BY p ORDER BY SUM(oi.qty) DESC")
    Page<Product> findBestSellers(Pageable pageable);
}
