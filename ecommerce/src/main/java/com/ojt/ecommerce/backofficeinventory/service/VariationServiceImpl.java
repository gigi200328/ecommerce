package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.repository.VariationRepository;
import com.ojt.ecommerce.entity.Variation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VariationServiceImpl implements VariationService {

	private final VariationRepository variationRepository;

	@Override
	public Variation createVariation(Variation variation) {

		if (variationRepository.existsByName(variation.getName())) {
			throw new RuntimeException("Variation already exists.");
		}

		return variationRepository.save(variation);
	}

	@Override
	public List<Variation> getAllVariations() {
		return variationRepository.findAll();
	}

	@Override
	public Variation getVariationById(Long id) {
		return variationRepository.findById(id).orElseThrow(() -> new RuntimeException("Variation not found."));
	}

	@Override
	public Variation updateVariation(Long id, Variation variation) {

		Variation oldVariation = getVariationById(id);

		oldVariation.setName(variation.getName());

		return variationRepository.save(oldVariation);
	}

	@Override
	public void deleteVariation(Long id) {

		Variation variation = getVariationById(id);

		variationRepository.delete(variation);
	}
}