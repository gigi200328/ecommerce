package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.LocationRegionDto;
import com.ojt.ecommerce.backofficeinventory.service.LocationMasterService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationMasterController {

	private final LocationMasterService locationMasterService;

	// ၁။ တိုင်း/ပြည်နယ် နှင့် မြို့နယ်အားလုံးကို ဆွဲထုတ်ရန်
	// GET /api/locations
	@GetMapping
	public ResponseEntity<List<LocationRegionDto>> getAllLocations() {
		return ResponseEntity.ok(locationMasterService.getAllLocations());
	}

	// ၂။ ရွေးချယ်လိုက်သော Region သို့မဟုတ် City အလိုက် မြို့နယ်များကိုသာ ဆွဲထုတ်ရန်
	// GET /api/locations/townships?regionOrCity=Yangon
	@GetMapping("/townships")
	public ResponseEntity<List<String>> getTownships(@RequestParam String regionOrCity) {
		return ResponseEntity.ok(locationMasterService.getTownshipsByRegionOrCity(regionOrCity));
	}
}