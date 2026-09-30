package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;

import com.ojt.ecommerce.entity.VariantOptionValue;

public interface VariantOptionValueService {

    VariantOptionValue createVariantOptionValue(VariantOptionValue variantOptionValue);

    List<VariantOptionValue> getAllVariantOptionValues();

    VariantOptionValue getVariantOptionValueById(Long id);

    List<VariantOptionValue> getOptionsByVariantId(Long variantId);

    List<VariantOptionValue> getVariantsByOptionId(Long optionId);

    VariantOptionValue updateVariantOptionValue(Long id, VariantOptionValue variantOptionValue);

    void deleteVariantOptionValue(Long id);
}
