package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductProductId(Long productId);
    List<ProductImage> findByProductProductIdIn(List<Long> productIds);
}