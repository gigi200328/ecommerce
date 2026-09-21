package com.ojt.ecommerce.backofficeinventory.service;


import java.util.List;

import org.springframework.stereotype.Service;

import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationRepository;
import com.ojt.ecommerce.entity.Variation;
import com.ojt.ecommerce.entity.VariationOption;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VariationOptionServiceImpl implements VariationOptionService {

    private final VariationOptionRepository variationOptionRepository;
    private final VariationRepository variationRepository;

    @Override
    public VariationOption createVariationOption(
            VariationOption variationOption) {

        Long variationId =
                variationOption.getVariation().getVariationId();

        // Check Variation exists
        Variation variation = variationRepository.findById(variationId)
                .orElseThrow(() ->
                        new RuntimeException("Variation not found."));

        // Check duplicate option
        if (variationOptionRepository
                .existsByVariation_VariationIdAndValue(
                        variationId,
                        variationOption.getValue())) {

            throw new RuntimeException(
                    "This option already exists for this variation.");
        }

        variationOption.setVariation(variation);

        return variationOptionRepository.save(variationOption);
    }

    @Override
    public List<VariationOption> getAllVariationOptions() {

        return variationOptionRepository.findAll();
    }

    @Override
    public VariationOption getVariationOptionById(Long id) {

        return variationOptionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Variation option not found."));
    }

    @Override
    public List<VariationOption> getOptionsByVariationId(
            Long variationId) {

        // Check Variation exists
        variationRepository.findById(variationId)
                .orElseThrow(() ->
                        new RuntimeException("Variation not found."));

        return variationOptionRepository
                .findByVariation_VariationId(variationId);
    }

    @Override
    public VariationOption updateVariationOption(
            Long id,
            VariationOption variationOption) {

        VariationOption existingOption =
                getVariationOptionById(id);

        Long variationId =
                existingOption.getVariation().getVariationId();

        // Check duplicate value
        if (!existingOption.getValue()
                .equalsIgnoreCase(variationOption.getValue())) {

            if (variationOptionRepository
                    .existsByVariation_VariationIdAndValue(
                            variationId,
                            variationOption.getValue())) {

                throw new RuntimeException(
                        "This option already exists for this variation.");
            }
        }

        existingOption.setValue(variationOption.getValue());

        return variationOptionRepository.save(existingOption);
    }

    @Override
    public void deleteVariationOption(Long id) {

        VariationOption variationOption =
                getVariationOptionById(id);

        variationOptionRepository.delete(variationOption);
    }
}