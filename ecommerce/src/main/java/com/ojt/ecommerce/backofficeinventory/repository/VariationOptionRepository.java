package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.VariationOption;

@Repository
public interface VariationOptionRepository
        extends JpaRepository<VariationOption, Long> {

    // Get options by variation
    List<VariationOption> findByVariation_VariationId(Long variationId);

    // Check duplicate option
    boolean existsByVariation_VariationIdAndValue(
            Long variationId,
            String value
    );
}