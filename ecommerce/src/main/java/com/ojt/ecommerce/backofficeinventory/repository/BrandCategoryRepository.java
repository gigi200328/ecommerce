
package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.entity.BrandCategory;

@Repository
public interface BrandCategoryRepository
        extends JpaRepository<BrandCategory, Long> {

    List<BrandCategory> findByBrandBrandId(Long brandId);

    boolean existsByCategoryCategoryId(Long categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            DELETE FROM BrandCategory bc
            WHERE bc.brand.brandId = :brandId
            """)
    int deleteByBrandBrandId(
            @Param("brandId") Long brandId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            DELETE FROM BrandCategory bc
            WHERE bc.category.categoryId = :categoryId
            """)
    int deleteByCategoryCategoryId(
            @Param("categoryId") Long categoryId
    );
}

