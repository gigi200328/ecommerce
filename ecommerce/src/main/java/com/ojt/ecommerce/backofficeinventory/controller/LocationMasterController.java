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

	// ၂။ ⭐️ အသစ်: ရွေးချယ်လိုက်သော တိုင်း/ပြည်နယ် (Region) အောက်ရှိ
	// ခရိုင်/မြို့များ (Cities) ကို ဆွဲထုတ်ရန်
	// GET /api/locations/cities?region=Yangon Region
	@GetMapping("/cities")
	public ResponseEntity<List<String>> getCities(@RequestParam String region) {
		return ResponseEntity.ok(locationMasterService.getCitiesByRegion(region));
	}

	// ၃။ ⭐️ ပြင်ဆင်: ရွေးချယ်လိုက်သော ခရိုင်/မြို့ (City) အောက်ရှိ မြို့နယ်များ
	// (Townships) ကိုသာ ဆွဲထုတ်ရန်
	// GET /api/locations/townships?city=Yangon City
	@GetMapping("/townships")
	public ResponseEntity<List<String>> getTownships(@RequestParam String city) {
		return ResponseEntity.ok(locationMasterService.getTownshipsByCity(city));
	}
}