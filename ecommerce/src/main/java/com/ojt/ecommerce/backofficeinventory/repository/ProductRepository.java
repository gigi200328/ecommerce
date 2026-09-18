package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByBrandBrandId(Long brandId, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}