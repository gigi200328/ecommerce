package com.ojt.ecommerce.backofficeinventory.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.enums.BrandStatus;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>, JpaSpecificationExecutor<Brand> {

	boolean existsByBrandName(String brandName);

	boolean existsByBrandNameAndBrandIdNot(String brandName, Long brandId);

	Page<Brand> findByStatus(BrandStatus status, Pageable pageable);
}