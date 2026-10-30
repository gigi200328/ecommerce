
package com.ojt.ecommerce.storefront.catalog.repository;

import com.ojt.ecommerce.entity.ProductVariantImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface G5ProductVariantImageRepository
        extends JpaRepository<ProductVariantImage, Long> {

    List<ProductVariantImage>
    findByProductVariantVariantIdIn(List<Long> variantIds);
}
