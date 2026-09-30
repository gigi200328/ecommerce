package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.enums.BrandStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findByStatus(BrandStatus status);
}
