package com.ojt.ecommerce.backofficeinventory.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.CreateZoneGroupRequest;
import com.ojt.ecommerce.backofficeinventory.dto.PaginatedResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.UpdateZoneRateRequest;
import com.ojt.ecommerce.backofficeinventory.dto.ZoneGroupResponse;
import com.ojt.ecommerce.backofficeinventory.mapper.ShipmentMapper;
import com.ojt.ecommerce.backofficeinventory.repository.DeliveryZoneRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ShippingRateRepository;
import com.ojt.ecommerce.entity.DeliveryZone;
import com.ojt.ecommerce.entity.ShippingRate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShippingService {

	private final DeliveryZoneRepository zoneRepository;
	private final ShippingRateRepository rateRepository;
	private final ShipmentMapper shipmentMapper;
	private final AdminNotificationService notificationService;

	// ၁။ Admin UI Table အတွက် Zone များကို zoneName အလိုက် Group ဖွဲ့၍ ဆွဲထုတ်ခြင်း
	@Transactional(readOnly = true)
	public PaginatedResponseDto<ZoneGroupResponse> getAllZoneGroups(String search, String statusFilter, int pageNo,
			int pageSize) {
		List<DeliveryZone> allZones = zoneRepository.findAllByOrderByZoneIdAsc();
		LocalDateTime now = LocalDateTime.now();
		Map<String, ZoneGroupResponse> groupedMap = new LinkedHashMap<>();

		for (DeliveryZone zone : allZones) {
			String groupKey = zone.getZoneName() + "_" + zone.getCity();

			ZoneGroupResponse group = groupedMap.computeIfAbsent(groupKey, k -> {
				Optional<ShippingRate> rateOpt = rateRepository.findCurrentRateByZoneId(zone.getZoneId(), now);
				return shipmentMapper.toInitialZoneGroupResponse(zone, rateOpt);
			});

			group.getZoneIds().add(zone.getZoneId());
			if (zone.getTownship() != null && !zone.getTownship().isBlank()) {
				group.getTownships().add(zone.getTownship());
			}
		}
		List<ZoneGroupResponse> allGroupedZones = new ArrayList<>(groupedMap.values());

		// (က) Memory ပေါ်တွင် Search နှင့် Status ကို စစ်ထုတ်ခြင်း
		List<ZoneGroupResponse> filteredZones = allGroupedZones.stream().filter(zone -> {
			boolean matchSearch = true;
			if (search != null && !search.trim().isEmpty()) {
				String lowerSearch = search.toLowerCase();
				matchSearch = (zone.getZoneName() != null && zone.getZoneName().toLowerCase().contains(lowerSearch))
						|| (zone.getCity() != null && zone.getCity().toLowerCase().contains(lowerSearch))
						|| zone.getTownships().stream().anyMatch(t -> t.toLowerCase().contains(lowerSearch));
			}

			boolean matchStatus = true;
			if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("ALL")) {
				matchStatus = zone.getStatus() != null && zone.getStatus().equalsIgnoreCase(statusFilter);
			}

			return matchSearch && matchStatus;
		}).toList();

		// (ခ) Memory ပေါ်တွင် Pagination ဖြတ်တောက်ခြင်း
		int totalElements = filteredZones.size();
		int totalPages = (int) Math.ceil((double) totalElements / pageSize);
		int fromIndex = Math.min(pageNo * pageSize, totalElements);
		int toIndex = Math.min((pageNo + 1) * pageSize, totalElements);

		List<ZoneGroupResponse> pagedZones = filteredZones.subList(fromIndex, toIndex);

		// DTO ထဲသို့ ထည့်၍ ပြန်ပေးခြင်း (Order မှာသုံးခဲ့သော PaginatedResponseDto)
		return new PaginatedResponseDto<>(pagedZones, pageNo, pageSize, totalElements, totalPages,
				toIndex == totalElements);
	}

	@Transactional
	public void toggleZoneStatus(String zoneName, String city, String newStatus) {
		List<DeliveryZone> zonesToUpdate = zoneRepository.findAllByOrderByZoneIdAsc().stream()
				.filter(z -> z.getZoneName().equalsIgnoreCase(zoneName) && z.getCity().equalsIgnoreCase(city)).toList();

		for (DeliveryZone zone : zonesToUpdate) {
			zone.setStatus(newStatus.toUpperCase());
			zone.setModifiedAt(LocalDateTime.now());
			zoneRepository.save(zone);
		}

		notificationService.sendCustomNotification("SHIPPING_UPDATE", "Zone Status Changed",
				"Delivery for " + zoneName + " in " + city + " is now " + newStatus.toUpperCase() + ".",
				"/admin/shipping-rates", null);
	}

	// ၂။ Zone အသစ် ဖန်တီးခြင်း (Default City Rate သို့မဟုတ်
	// မြို့နယ်အလိုက်ခွဲသိမ်းခြင်း)
	@Transactional
	public void createZoneGroup(CreateZoneGroupRequest request) {
		LocalDateTime now = LocalDateTime.now();

		if (zoneRepository.existsByZoneNameIgnoreCase(request.getZoneName())) {
			throw new IllegalArgumentException(
					"A zone named '" + request.getZoneName() + "' already exists. Please choose a different name.");
		}
		// City ရှေ့ ၃ လုံးဖြင့် Auto-generate လုပ်မည်(ဥပမာ - YAN-101)
		String baseCode = request.getCity().substring(0, Math.min(3, request.getCity().length())).toUpperCase() + "-"
				+ (System.currentTimeMillis() % 1000);

		if (request.getTownships() != null && !request.getTownships().isEmpty()) {
			// ၁။ တခြား Zone မှာ ပါပြီးသား မြို့နယ်များကို စုရန် List တစ်ခုဆောက်မည်
			List<String> duplicatedTownships = new ArrayList<>();

			// ၂။ မြို့နယ်အားလုံးကို အရင် စစ်ဆေးမည်
			for (String township : request.getTownships()) {
				if (zoneRepository.existsByCityAndTownship(request.getCity(), township)) {
					duplicatedTownships.add(township);
				}
			}

			// ၃။ ပါပြီးသား မြို့နယ်တွေ ရှိနေရင် စုပြီး Error တစ်ခါတည်း ပစ်မည်
			if (!duplicatedTownships.isEmpty()) {
				String joinedTownships = String.join(", ", duplicatedTownships);
				throw new IllegalArgumentException("The following townships are already assigned to another zone: "
						+ joinedTownships + ". Please edit the existing zone instead.");
			}

			// ၄။ Error မရှိမှသာ Entity များကို Save လုပ်မည်
			int index = 1;
			for (String township : request.getTownships()) {
				String generatedCode = String.format("%s-%02d", baseCode, index++);

				DeliveryZone savedZone = zoneRepository
						.save(shipmentMapper.toDeliveryZoneEntity(request, generatedCode, township, now));

				rateRepository.save(shipmentMapper.toShippingRateEntity(savedZone, request.getShippingFee(),
						request.getEstimatedDays(), now));
			}
		}

		// ခ။ Checkbox ခြစ်ထားပါက ကျန်သော မြို့နယ်အားလုံးအတွက် Default Zone ကိုပါ
		// တစ်ဆက်တည်း တည်ဆောက်မည်
		if (request.isHasDefaultRate()) {
			zoneRepository.findByCityAndTownshipIsNull(request.getCity()).ifPresent(z -> {
				throw new IllegalArgumentException(request.getCity() + " already has a default zone.");
			});

			String defaultCode = baseCode + "-DEF";

			// request ကို ပုံစံခေတ္တပြောင်း၍ Mapper ထဲသို့ ထည့်ပေးခြင်း (Zone Name
			// နောက်တွင် Default စာသားကပ်ရန်)
			String originalZoneName = request.getZoneName();
			request.setZoneName(originalZoneName + " (Default)");

			// Mapper ကို အသုံးပြု၍ Default Zone အတွက် Entity တည်ဆောက်ခြင်း (Township
			// နေရာတွင် null ပေးပို့မည်)
			DeliveryZone savedDefaultZone = zoneRepository
					.save(shipmentMapper.toDeliveryZoneEntity(request, defaultCode, null, now));

			// Default Shipping Fee နှင့် Estimated Days များဖြင့် Rate Entity တည်ဆောက်ခြင်း
			rateRepository.save(shipmentMapper.toShippingRateEntity(savedDefaultZone, request.getDefaultShippingFee(),
					request.getDefaultEstimatedDays(), now));

			// request အတွင်းရှိ Data ကို မူလအတိုင်း ပြန်ထားရန်
			request.setZoneName(originalZoneName);
		}

		notificationService.sendCustomNotification("SHIPPING_CREATE", "New Shipping Zone Created",
				"A new delivery zone '" + request.getZoneName() + "' has been successfully created.",
				"/admin/shipping-rates", null);
	}

	// ၃။ ရှိပြီးသား Zone Group ၏ ဈေးနှုန်း (Shipping Fee) နှင့် Status
	// ကိုပြင်ဆင်ခြင်း
	@Transactional
	public void updateZoneGroupRate(UpdateZoneRateRequest request) {
		LocalDateTime now = LocalDateTime.now();

		// ⭐️ အဆင့် (၁): Soft Delete လုပ်မည့် Zone များရှိလျှင် ဖယ်ရှားမည်
		if (request.getRemovedZoneIds() != null && !request.getRemovedZoneIds().isEmpty()) {
			List<DeliveryZone> removedZones = zoneRepository.findAllById(request.getRemovedZoneIds());

			for (DeliveryZone removedZone : removedZones) {
				removedZone.setTownship(removedZone.getTownship() + "_DELETED_" + removedZone.getZoneId());
				removedZone.setZoneName("DELETED_ZONE_" + removedZone.getZoneId());
				removedZone.setStatus("INACTIVE");
				zoneRepository.save(removedZone);

				rateRepository.findCurrentRateByZoneId(removedZone.getZoneId(), now).ifPresent(oldRate -> {
					shipmentMapper.deactivateRateEntity(oldRate, now);
					rateRepository.save(oldRate);
				});
			}
		} // <-- အဆင့် (၁) ဤနေရာတွင် ပြီးဆုံးပါသည်။ (Curly brace အပိတ် မှန်ကန်ပါပြီ)

		// ⭐️ အဆင့် (၂): ကျန်ရှိနေသော Zone များကို Update လုပ်ခြင်း (တစ်ခါတည်းသာ
		// ခေါ်ပါမည်)
		List<DeliveryZone> zones = zoneRepository.findAllById(request.getZoneIds());
		Set<String> updatedZoneNames = new HashSet<>();

		for (DeliveryZone zone : zones) {
			// DeliveryZone ၏ Name နှင့် Status ကို DTO တွင် ပါလာသလောက် Update လုပ်မည်
			shipmentMapper.updateZoneEntity(zone, request, now);
			zoneRepository.save(zone);

			if (zone.getZoneName() != null) {
				updatedZoneNames.add(zone.getZoneName());
			}

			// Shipping Fee နှင့် Estimated Days အတွက် Null စစ်ဆေးသည့် Fallback Logic
			BigDecimal finalFee = request.getShippingFee();
			Integer finalDays = request.getEstimatedDays();

			Optional<ShippingRate> oldRateOpt = rateRepository.findCurrentRateByZoneId(zone.getZoneId(), now);

			if (oldRateOpt.isPresent()) {
				ShippingRate oldRate = oldRateOpt.get();

				// Request တွင် Fee (သို့) Days အလွတ်ဖြစ်နေပါက Rate အဟောင်း၏ တန်ဖိုးများကို
				// ပြန်ယူမည်
				if (finalFee == null) {
					finalFee = oldRate.getFee();
				}
				if (finalDays == null) {
					finalDays = oldRate.getEstimatedDays();
				}

				// Rate အဟောင်းကို Inactive အဖြစ် ပိတ်မည်
				shipmentMapper.deactivateRateEntity(oldRate, now);
				rateRepository.save(oldRate);
			}

			// အကယ်၍ အဟောင်းလည်းမရှိ၊ အသစ်လည်း မပို့ခဲ့လျှင် Error မတက်စေရန် Default (0 / 1)
			// ထားမည်
			if (finalFee == null) {
				finalFee = BigDecimal.ZERO;
			}
			if (finalDays == null) {
				finalDays = 1;
			}

			// ⭐️ အရေးကြီးသည်: Fallback ပြုလုပ်ထားသော finalFee, finalDays ဖြင့်သာ Rate
			// အသစ်ကို သိမ်းမည် ⭐️
			rateRepository.save(shipmentMapper.toShippingRateEntity(zone, finalFee, finalDays, now));
		}

		if (!updatedZoneNames.isEmpty()) {
			String joinedNames = String.join(", ", updatedZoneNames);

			notificationService.sendCustomNotification("SHIPPING_UPDATE", "Shipping Rates Updated",
					"Shipping parameters have been updated for: " + joinedNames, "/admin/shipping-rates", null);
		}
	}

	// ၄။ သတ်မှတ်ပြီးသား မြို့နယ်များကို UI တွင် Disabled
	// ပြနိုင်ရန်စာရင်းထုတ်ပေးခြင်း
	@Transactional(readOnly = true)
	public List<String> getAssignedTownshipsByCity(String city) {
		return zoneRepository
				.findAllByOrderByZoneIdAsc().stream().filter(z -> z.getCity().equalsIgnoreCase(city)
						&& z.getTownship() != null && "ACTIVE".equalsIgnoreCase(z.getStatus()))
				.map(DeliveryZone::getTownship).toList();
	}

	@Transactional(readOnly = true)
	public Map<String, Long> getZoneStats() {
		List<DeliveryZone> allZones = zoneRepository.findAllByOrderByZoneIdAsc();
		Map<String, String> groupStatusMap = new LinkedHashMap<>();

		// Zone များကို Name နှင့် City တူရာ Group ဖွဲ့မည်
		for (DeliveryZone zone : allZones) {
			if (zone.getTownship() == null
					|| (zone.getZoneName() != null && zone.getZoneName().startsWith("DELETED_ZONE"))) {
				continue;
			}

			String groupKey = zone.getZoneName() + "_" + zone.getCity();
			groupStatusMap.putIfAbsent(groupKey, zone.getStatus());
		}

		long total = groupStatusMap.size();
		long active = groupStatusMap.values().stream().filter(status -> "ACTIVE".equalsIgnoreCase(status)).count();
		long inactive = total - active;

		return Map.of("total", total, "active", active, "inactive", inactive);
	}
}
