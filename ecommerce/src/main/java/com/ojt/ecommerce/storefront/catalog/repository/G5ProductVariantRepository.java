package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductProductId(Long productId);
    List<ProductVariant> findByProductProductIdIn(List<Long> productIds);
}