package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.backofficeinventory.dto.CreateZoneGroupRequest;
import com.ojt.ecommerce.backofficeinventory.dto.PaginatedResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.UpdateZoneRateRequest;
import com.ojt.ecommerce.backofficeinventory.dto.ZoneGroupResponse;
import com.ojt.ecommerce.backofficeinventory.mapper.ShipmentMapper;
import com.ojt.ecommerce.backofficeinventory.repository.DeliveryZoneRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ShippingRateRepository;
import com.ojt.ecommerce.entity.DeliveryZone;
import com.ojt.ecommerce.entity.ShippingRate;

@ExtendWith(MockitoExtension.class)
class ShippingServiceTest {

	@Mock
	private DeliveryZoneRepository zoneRepository;

	@Mock
	private ShippingRateRepository rateRepository;

	@Mock
	private ShipmentMapper shipmentMapper;

	@Mock
	private AdminNotificationService notificationService;

	@InjectMocks
	private ShippingService shippingService;

	private DeliveryZone zone1;
	private DeliveryZone zone2;
	private ShippingRate rate1;

	@BeforeEach
	void setUp() {
		zone1 = new DeliveryZone();
		zone1.setZoneId(1L);
		zone1.setZoneName("Yangon Downtown");
		zone1.setCity("Yangon");
		zone1.setTownship("Ahlone");
		zone1.setStatus("ACTIVE");

		zone2 = new DeliveryZone();
		zone2.setZoneId(2L);
		zone2.setZoneName("Yangon Downtown");
		zone2.setCity("Yangon");
		zone2.setTownship("Dagon");
		zone2.setStatus("ACTIVE");

		rate1 = new ShippingRate();
		rate1.setShippingRateId(101L);
		rate1.setFee(new BigDecimal("5000"));
		rate1.setEstimatedDays(3);
		rate1.setZone(zone1);
	}

	@Test
	@DisplayName("getAllZoneGroups() - Should return grouped zones with pagination")
	void testGetAllZoneGroups() {
		// Arrange
		when(zoneRepository.findAllByOrderByZoneIdAsc()).thenReturn(Arrays.asList(zone1, zone2));

		ZoneGroupResponse groupResponse = new ZoneGroupResponse();
		groupResponse.setZoneName("Yangon Downtown");
		groupResponse.setCity("Yangon");
		groupResponse.setStatus("ACTIVE");

		// ⭐️ ပြင်ဆင်ချက်: NullPointerException မတက်စေရန် List များကို ကြိုတင်
		// Initialize လုပ်ပေးခြင်း ⭐️
		groupResponse.setZoneIds(new java.util.ArrayList<>());
		groupResponse.setTownships(new java.util.ArrayList<>());

		when(rateRepository.findCurrentRateByZoneId(anyLong(), any(LocalDateTime.class)))
				.thenReturn(Optional.of(rate1));
		when(shipmentMapper.toInitialZoneGroupResponse(any(DeliveryZone.class), any(Optional.class)))
				.thenReturn(groupResponse);

		// Act
		PaginatedResponseDto<ZoneGroupResponse> result = shippingService.getAllZoneGroups(null, "ALL", 0, 10);

		// Assert
		assertNotNull(result);
		assertEquals(1, result.getContent().size()); // Grouped into 1 item
		assertEquals("Yangon Downtown", result.getContent().get(0).getZoneName());
		assertTrue(result.getContent().get(0).getTownships().contains("Ahlone"));
	}

	@Test
	@DisplayName("toggleZoneStatus() - Should update status and trigger notification")
	void testToggleZoneStatus() {
		// Arrange
		when(zoneRepository.findAllByOrderByZoneIdAsc()).thenReturn(Arrays.asList(zone1, zone2));

		// Act
		shippingService.toggleZoneStatus("Yangon Downtown", "Yangon", "INACTIVE");

		// Assert
		assertEquals("INACTIVE", zone1.getStatus());
		assertEquals("INACTIVE", zone2.getStatus());
		verify(zoneRepository, times(2)).save(any(DeliveryZone.class));
		verify(notificationService, times(1)).sendCustomNotification(eq("SHIPPING_UPDATE"), anyString(), anyString(),
				anyString(), isNull());
	}

	@Test
	@DisplayName("createZoneGroup() - Should throw exception if zone name already exists")
	void testCreateZoneGroup_NameExists() {
		// Arrange
		CreateZoneGroupRequest request = new CreateZoneGroupRequest();
		request.setZoneName("Yangon Downtown");

		when(zoneRepository.existsByZoneNameIgnoreCase("Yangon Downtown")).thenReturn(true);

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> {
			shippingService.createZoneGroup(request);
		});
	}

	@Test
	@DisplayName("createZoneGroup() - Should save new zones successfully")
	void testCreateZoneGroup_Success() {
		// Arrange
		CreateZoneGroupRequest request = new CreateZoneGroupRequest();
		request.setZoneName("Mandalay Central");
		request.setCity("Mandalay");
		request.setTownships(List.of("Chanayethazan", "Aungmyethazan"));
		request.setShippingFee(new BigDecimal("6000"));
		request.setEstimatedDays(4);

		when(zoneRepository.existsByZoneNameIgnoreCase("Mandalay Central")).thenReturn(false);
		when(zoneRepository.existsByCityAndTownship(anyString(), anyString())).thenReturn(false);

		when(shipmentMapper.toDeliveryZoneEntity(any(), anyString(), anyString(), any())).thenReturn(zone1);
		when(zoneRepository.save(any(DeliveryZone.class))).thenReturn(zone1);

		// Act
		shippingService.createZoneGroup(request);

		// Assert
		verify(zoneRepository, times(2)).save(any(DeliveryZone.class)); // Saved for 2 townships
		verify(rateRepository, times(2)).save(any());
		verify(notificationService, times(1)).sendCustomNotification(eq("SHIPPING_CREATE"), anyString(), anyString(),
				anyString(), isNull());
	}

	@Test
	@DisplayName("updateZoneGroupRate() - Should handle Fallback Logic for null fee and days")
	void testUpdateZoneGroupRate_WithFallback() {
		// Arrange
		UpdateZoneRateRequest request = new UpdateZoneRateRequest();
		request.setZoneIds(List.of(1L));
		request.setShippingFee(null); // Simulated empty input
		request.setEstimatedDays(null);

		when(zoneRepository.findAllById(request.getZoneIds())).thenReturn(List.of(zone1));
		when(rateRepository.findCurrentRateByZoneId(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(rate1));

		ShippingRate newRate = new ShippingRate();
		// Since fee is null, the mock should use old values (5000 and 3)
		when(shipmentMapper.toShippingRateEntity(eq(zone1), eq(new BigDecimal("5000")), eq(3),
				any(LocalDateTime.class))).thenReturn(newRate);

		// Act
		shippingService.updateZoneGroupRate(request);

		// Assert
		verify(shipmentMapper, times(1)).deactivateRateEntity(eq(rate1), any(LocalDateTime.class));
		verify(rateRepository, times(2)).save(any(ShippingRate.class)); // 1 for old rate deactivation, 1 for new rate
	}

	@Test
	@DisplayName("getAssignedTownshipsByCity() - Should return list of assigned active townships")
	void testGetAssignedTownshipsByCity() {
		// Arrange
		when(zoneRepository.findAllByOrderByZoneIdAsc()).thenReturn(Arrays.asList(zone1, zone2));

		// Act
		List<String> result = shippingService.getAssignedTownshipsByCity("Yangon");

		// Assert
		assertEquals(2, result.size());
		assertTrue(result.containsAll(List.of("Ahlone", "Dagon")));
	}

	@Test
	@DisplayName("getZoneStats() - Should group zones by Name and City and count correctly")
	void testGetZoneStats() {
		// Arrange
		DeliveryZone inactiveZone = new DeliveryZone();
		inactiveZone.setZoneId(3L);
		inactiveZone.setZoneName("Mandalay South");
		inactiveZone.setCity("Mandalay");
		inactiveZone.setTownship("Maharaungmye");
		inactiveZone.setStatus("INACTIVE");

		when(zoneRepository.findAllByOrderByZoneIdAsc()).thenReturn(Arrays.asList(zone1, zone2, inactiveZone));

		// Act
		Map<String, Long> stats = shippingService.getZoneStats();

		// Assert
		// zone1 & zone2 form 1 Group (ACTIVE)
		// inactiveZone forms 1 Group (INACTIVE)
		assertEquals(2L, stats.get("total"));
		assertEquals(1L, stats.get("active"));
		assertEquals(1L, stats.get("inactive"));
	}
}