package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.VariantOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5VariantOptionValueRepository extends JpaRepository<VariantOptionValue, Long> {
    List<VariantOptionValue> findByVariantVariantId(Long variantId);
    List<VariantOptionValue> findByVariantVariantIdIn(List<Long> variantIds);
}