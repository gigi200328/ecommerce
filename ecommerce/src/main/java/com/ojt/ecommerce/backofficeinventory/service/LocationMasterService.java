package com.ojt.ecommerce.backofficeinventory.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	// တိုင်း/ပြည်နယ် သို့မဟုတ် City အမည်ဖြင့် မြို့နယ်စာရင်းကိုသာ သီးသန့် ရယူရန်
	public List<String> getTownshipsByRegionOrCity(String query) {
		return cachedLocations.stream()
				.filter(loc -> loc.getRegionOrState().equalsIgnoreCase(query) || loc.getCity().equalsIgnoreCase(query))
				.findFirst().map(LocationRegionDto::getTownships).orElse(Collections.emptyList());
	}
}