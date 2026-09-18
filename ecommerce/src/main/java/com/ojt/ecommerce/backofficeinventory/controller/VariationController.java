package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.service.VariationService;
import com.ojt.ecommerce.entity.Variation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/variations")
@RequiredArgsConstructor
public class VariationController {

	private final VariationService variationService;

	@PostMapping
	public Variation createVariation(@RequestBody Variation variation) {
		return variationService.createVariation(variation);
	}

	@GetMapping
	public List<Variation> getAllVariations() {
		return variationService.getAllVariations();
	}

	@GetMapping("/{id}")
	public Variation getVariationById(@PathVariable Long id) {
		return variationService.getVariationById(id);
	}

	@PutMapping("/{id}")
	public Variation updateVariation(@PathVariable Long id, @RequestBody Variation variation) {
		return variationService.updateVariation(id, variation);
	}

	@DeleteMapping("/{id}")
	public String deleteVariation(@PathVariable Long id) {

		variationService.deleteVariation(id);

		return "Variation deleted successfully.";
	}
}