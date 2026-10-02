package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.VariantOptionValue;

@Repository
public interface VariantOptionValueRepository extends JpaRepository<VariantOptionValue, Long> {

    // Check duplicate combination
    boolean existsByVariant_VariantIdAndOption_OptionId(Long variantId, Long optionId);

    // Check duplicate combination excluding current ID for update
    boolean existsByVariant_VariantIdAndOption_OptionIdAndIdNot(Long variantId, Long optionId, Long id);

    // Find all options assigned to a specific variant
    List<VariantOptionValue> findByVariant_VariantId(Long variantId);

    // Find all variants that use a specific option
    List<VariantOptionValue> findByOption_OptionId(Long optionId);
}
