package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationRepository;
import com.ojt.ecommerce.entity.Variation;
import com.ojt.ecommerce.entity.VariationOption;

public class VariationOptionServiceImpl implements VariationOptionService{

	private final VariationOptionRepository variationOptionRepository;
	private final VariationRepository variationRepository;
	
	@Override
	public VariationOption createVariationOption(
			VariationOption variationOption) {
		
		Long variationId =
				variationOption.getVariation().getVariationId();
		
			Variation variation =variationRepository.findById(variationId)
					.orElseThrow(()->
					new Rn)
					
	}
	
}
