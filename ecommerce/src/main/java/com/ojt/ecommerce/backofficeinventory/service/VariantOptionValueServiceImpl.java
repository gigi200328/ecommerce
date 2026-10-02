package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariantOptionValueRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.VariantOptionValue;
import com.ojt.ecommerce.entity.VariationOption;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VariantOptionValueServiceImpl implements VariantOptionValueService {

    private final VariantOptionValueRepository variantOptionValueRepository;
    private final ProductVariantRepository productVariantRepository;
    private final VariationOptionRepository variationOptionRepository;

    @Override
    public VariantOptionValue createVariantOptionValue(VariantOptionValue variantOptionValue) {
        if (variantOptionValue.getVariant() == null || variantOptionValue.getVariant().getVariantId() == null) {
            throw new RuntimeException("Product Variant ID is required.");
        }
        if (variantOptionValue.getOption() == null || variantOptionValue.getOption().getOptionId() == null) {
            throw new RuntimeException("Variation Option ID is required.");
        }

        Long variantId = variantOptionValue.getVariant().getVariantId();
        Long optionId = variantOptionValue.getOption().getOptionId();

        // Check ProductVariant exists
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + variantId));

        // Check VariationOption exists
        VariationOption option = variationOptionRepository.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Variation option not found with id: " + optionId));

        // Prevent duplicate combination (variant_id + option_id)
        if (variantOptionValueRepository.existsByVariant_VariantIdAndOption_OptionId(variantId, optionId)) {
            throw new RuntimeException("This variation option is already assigned to the product variant.");
        }

        variantOptionValue.setVariant(variant);
        variantOptionValue.setOption(option);

        return variantOptionValueRepository.save(variantOptionValue);
    }

    @Override
    public List<VariantOptionValue> getAllVariantOptionValues() {
        return variantOptionValueRepository.findAll();
    }

    @Override
    public VariantOptionValue getVariantOptionValueById(Long id) {
        return variantOptionValueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Variant option value mapping not found with id: " + id));
    }

    @Override
    public List<VariantOptionValue> getOptionsByVariantId(Long variantId) {
        // Check ProductVariant exists
        if (!productVariantRepository.existsById(variantId)) {
            throw new RuntimeException("Product variant not found with id: " + variantId);
        }

        return variantOptionValueRepository.findByVariant_VariantId(variantId);
    }

    @Override
    public List<VariantOptionValue> getVariantsByOptionId(Long optionId) {
        // Check VariationOption exists
        if (!variationOptionRepository.existsById(optionId)) {
            throw new RuntimeException("Variation option not found with id: " + optionId);
        }

        return variantOptionValueRepository.findByOption_OptionId(optionId);
    }

    @Override
    public VariantOptionValue updateVariantOptionValue(Long id, VariantOptionValue variantOptionValue) {
        VariantOptionValue existing = getVariantOptionValueById(id);

        Long variantId = (variantOptionValue.getVariant() != null && variantOptionValue.getVariant().getVariantId() != null)
                ? variantOptionValue.getVariant().getVariantId()
                : existing.getVariant().getVariantId();

        Long optionId = (variantOptionValue.getOption() != null && variantOptionValue.getOption().getOptionId() != null)
                ? variantOptionValue.getOption().getOptionId()
                : existing.getOption().getOptionId();

        // Prevent duplicate combination excluding current ID
        if (variantOptionValueRepository.existsByVariant_VariantIdAndOption_OptionIdAndIdNot(variantId, optionId, id)) {
            throw new RuntimeException("This variation option is already assigned to the product variant.");
        }

        // Validate and update ProductVariant
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product variant not found with id: " + variantId));
        existing.setVariant(variant);

        // Validate and update VariationOption
        VariationOption option = variationOptionRepository.findById(optionId)
                .orElseThrow(() -> new RuntimeException("Variation option not found with id: " + optionId));
        existing.setOption(option);

        return variantOptionValueRepository.save(existing);
    }

    @Override
    public void deleteVariantOptionValue(Long id) {
        VariantOptionValue mapping = getVariantOptionValueById(id);
        variantOptionValueRepository.delete(mapping);
    }
}
