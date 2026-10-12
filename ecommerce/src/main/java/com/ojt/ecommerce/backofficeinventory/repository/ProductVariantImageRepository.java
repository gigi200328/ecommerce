package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.ProductVariantImage;

@Repository
public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, Long> {

    List<ProductVariantImage> findByProductVariantVariantId(Long variantId);

    void deleteByProductVariantVariantId(Long variantId);
}
