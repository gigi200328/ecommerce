package com.ojt.ecommerce.backofficeinventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.enums.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>,JpaSpecificationExecutor<Product>{

    Page<Product> findByCategoryCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByBrandBrandId(Long brandId, Pageable pageable);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}