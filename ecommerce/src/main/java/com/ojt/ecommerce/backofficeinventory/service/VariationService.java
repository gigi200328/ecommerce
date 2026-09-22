package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.entity.Variation;

import java.util.List;

public interface VariationService {

	Variation createVariation(Variation variation);

	List<Variation> getAllVariations();

	Variation getVariationById(Long id);

	Variation updateVariation(Long id, Variation variation);

	void deleteVariation(Long id);

}