package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;

import com.ojt.ecommerce.entity.VariationOption;

public interface VariationOptionService {
	
	VariationOption createVariationOption(VariationOption variationoption);
	
	List<VariationOption> getAllVariationOptions();
	
	VariationOption getVariationOptionById(Long id);
	
	List<VariationOption> getOptionsByVariationId(Long variationId);
	
	VariationOption updateVariationOption(
			Long id,
			VariationOption variationOption
			);
	
	void deleteVariationOption(Long id);
}
