package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.CreateZoneGroupRequest;
import com.ojt.ecommerce.backofficeinventory.dto.PaginatedResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.UpdateZoneRateRequest;
import com.ojt.ecommerce.backofficeinventory.dto.ZoneGroupResponse;
import com.ojt.ecommerce.backofficeinventory.service.ShippingService;

import lombok.RequiredArgsConstructor;

@RestController("backofficeShippingController") // Storefront ရှိ ShippingController နှင့် Bean အမည်မထပ်စေရန်
@RequestMapping("/api/backoffice/shipping")
@RequiredArgsConstructor
public class ShippingController {

	private final ShippingService shippingService;

	@GetMapping("/zones")
	public ResponseEntity<PaginatedResponseDto<ZoneGroupResponse>> getAllZones(
			@RequestParam(value = "search", required = false) String search,
			@RequestParam(value = "status", required = false) String status, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int pageSize) {

		PaginatedResponseDto<ZoneGroupResponse> zones = shippingService.getAllZoneGroups(search, status, page,
				pageSize);
		return ResponseEntity.ok(zones);
	}

	@PostMapping("/zones")
	public ResponseEntity<Map<String, String>> createZoneGroup(@RequestBody CreateZoneGroupRequest request) {
		shippingService.createZoneGroup(request);
		return ResponseEntity.ok(Map.of("message", "Delivery zone and shipping rates created successfully"));
	}

	@PutMapping("/zones/rate")
	public ResponseEntity<Map<String, String>> updateZoneRate(@RequestBody UpdateZoneRateRequest request) {
		shippingService.updateZoneGroupRate(request);
		return ResponseEntity.ok(Map.of("message", "Shipping rate updated successfully"));
	}

	@GetMapping("/zones/assigned-townships")
	public ResponseEntity<List<String>> getAssignedTownships(@RequestParam String city) {
		return ResponseEntity.ok(shippingService.getAssignedTownshipsByCity(city));
	}

	@PatchMapping("/zones/{zoneName}/status")
	public ResponseEntity<?> toggleZoneStatus(@PathVariable String zoneName, @RequestParam String city,
			@RequestParam String newStatus) {

		shippingService.toggleZoneStatus(zoneName, city, newStatus);
		return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
	}

	@GetMapping("/zones/stats")
	public ResponseEntity<?> getZoneStats() {
		return ResponseEntity.ok(shippingService.getZoneStats());
	}
}