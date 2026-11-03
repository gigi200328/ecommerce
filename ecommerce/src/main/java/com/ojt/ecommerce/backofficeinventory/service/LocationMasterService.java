package com.ojt.ecommerce.backofficeinventory.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.backofficeinventory.dto.LocationCityDto;
import com.ojt.ecommerce.backofficeinventory.dto.LocationRegionDto;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationMasterService {

	private final ObjectMapper objectMapper;
	private List<LocationRegionDto> cachedLocations = Collections.emptyList();

	@PostConstruct
	public void init() {
		try {
			ClassPathResource resource = new ClassPathResource("myanmar-locations.json");
			try (InputStream inputStream = resource.getInputStream()) {
				cachedLocations = objectMapper.readValue(inputStream, new TypeReference<List<LocationRegionDto>>() {
				});
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load myanmar-locations.json master data", e);
		}
	}

	// တိုင်းနှင့်ပြည်နယ် အားလုံးနှင့် မြို့နယ်စာရင်းအားလုံးကို ရယူရန်
	public List<LocationRegionDto> getAllLocations() {
		return cachedLocations;
	}

	// ၂။ ⭐️ အသစ်: တိုင်း/ပြည်နယ် အမည်ဖြင့် ၎င်းအောက်ရှိ ခရိုင်/မြို့ (City)
	// အမည်များကိုသာ ရယူရန်
	public List<String> getCitiesByRegion(String region) {
		return cachedLocations.stream().filter(loc -> loc.getRegionOrState().equalsIgnoreCase(region)).findFirst()
				.map(loc -> loc.getCities().stream().map(LocationCityDto::getCityName).toList())
				.orElse(Collections.emptyList());
	}

	// ၃။ ⭐️ ပြင်ဆင်: ခရိုင်/မြို့ (City) အမည်ဖြင့် ၎င်းအောက်ရှိ မြို့နယ် (Township)
	// များကိုသာ ရယူရန်
	public List<String> getTownshipsByCity(String city) {
		return cachedLocations.stream().flatMap(loc -> loc.getCities().stream()) // Region အားလုံးအောက်ရှိ City များကို
																					// ဖြန့်ထုတ်ခြင်း
				.filter(c -> c.getCityName().equalsIgnoreCase(city)).findFirst().map(LocationCityDto::getTownships)
				.orElse(Collections.emptyList());
	}
}