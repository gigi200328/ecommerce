package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.ProductTag;
import com.ojt.ecommerce.entity.id.ProductTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5ProductTagRepository extends JpaRepository<ProductTag, ProductTagId> {
    List<ProductTag> findByProductProductId(Long productId);
    List<ProductTag> findByProductProductIdIn(List<Long> productIds);
    List<ProductTag> findByTagTagNameIn(List<String> tags);
}
