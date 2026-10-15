package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.CreateZoneGroupRequest;
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

	// ၁။ Admin UI Table အတွက် Zone များကို zoneName အလိုက် Group ဖွဲ့၍ ဆွဲထုတ်ခြင်း
	@Transactional(readOnly = true)
	public List<ZoneGroupResponse> getAllZoneGroups() {
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

		return new ArrayList<>(groupedMap.values());
	}

	// ၂။ Zone အသစ် ဖန်တီးခြင်း (Default City Rate သို့မဟုတ်
	// မြို့နယ်အလိုက်ခွဲသိမ်းခြင်း)
	@Transactional
	public void createZoneGroup(CreateZoneGroupRequest request) {
		LocalDateTime now = LocalDateTime.now();

		// City ရှေ့ ၃ လုံးဖြင့် Auto-generate လုပ်မည်(ဥပမာ - YAN-101)
		String baseCode = request.getCity().substring(0, Math.min(3, request.getCity().length())).toUpperCase() + "-"
				+ (System.currentTimeMillis() % 1000);

		// Townships မပါလျှင် တစ်မြို့လုံးအတွက် Default Rate (township = null) အဖြစ်
		// သိမ်းမည်
		if (request.getTownships() == null || request.getTownships().isEmpty()) {
			zoneRepository.findByCityAndTownshipIsNull(request.getCity()).ifPresent(z -> {
				throw new IllegalArgumentException(request.getCity() + " already have a default zone.");
			});

			String defaultCode = request.getZoneCode() + "-DEF";
			DeliveryZone defaultZone = zoneRepository
					.save(shipmentMapper.toDeliveryZoneEntity(request, defaultCode, null, now));
			rateRepository.save(shipmentMapper.toShippingRateEntity(defaultZone, request.getShippingFee(),
					request.getEstimatedDays(), now));
			return;
		}

		// ရွေးချယ်ထားသော မြို့နယ်တစ်ခုချင်းစီအတွက် Row တစ်ကြောင်းစီ ခွဲသိမ်းမည်
		int index = 1;
		for (String township : request.getTownships()) {
			zoneRepository.findByCityAndTownship(request.getCity(), township).ifPresent(existing -> {
				throw new IllegalArgumentException(
						township + " is '" + existing.getZoneName() + "' already in the zone.");
			});

			String generatedCode = String.format("%s-%02d", baseCode, index++);
			DeliveryZone savedZone = zoneRepository
					.save(shipmentMapper.toDeliveryZoneEntity(request, generatedCode, township, now));
			rateRepository.save(shipmentMapper.toShippingRateEntity(savedZone, request.getShippingFee(),
					request.getEstimatedDays(), now));
		}
	}

	// ၃။ ရှိပြီးသား Zone Group ၏ ဈေးနှုန်း (Shipping Fee) နှင့် Status
	// ကိုပြင်ဆင်ခြင်း
	@Transactional
	public void updateZoneGroupRate(UpdateZoneRateRequest request) {
		LocalDateTime now = LocalDateTime.now();
		List<DeliveryZone> zones = zoneRepository.findAllById(request.getZoneIds());

		for (DeliveryZone zone : zones) {
			shipmentMapper.updateZoneEntity(zone, request, now);
			zoneRepository.save(zone);

			rateRepository.findCurrentRateByZoneId(zone.getZoneId(), now).ifPresent(oldRate -> {
				shipmentMapper.deactivateRateEntity(oldRate, now);
				rateRepository.save(oldRate);
			});

			rateRepository.save(shipmentMapper.toShippingRateEntity(zone, request.getShippingFee(),
					request.getEstimatedDays(), now));
		}
	}

	// ၄။ သတ်မှတ်ပြီးသား မြို့နယ်များကို UI တွင် Disabled
	// ပြနိုင်ရန်စာရင်းထုတ်ပေးခြင်း
	@Transactional(readOnly = true)
	public List<String> getAssignedTownshipsByCity(String city) {
		return zoneRepository.findAllByOrderByZoneIdAsc().stream()
				.filter(z -> z.getCity().equalsIgnoreCase(city) && z.getTownship() != null)
				.map(DeliveryZone::getTownship).toList();
	}
}
