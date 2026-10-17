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

		boolean isZoneNameExists = zoneRepository.existsByCityAndZoneName(request.getCity(), request.getZoneName());
		if (isZoneNameExists) {
			throw new IllegalArgumentException(
					"A zone named '" + request.getZoneName() + "' already exists in " + request.getCity() + ".");
		}
		// City ရှေ့ ၃ လုံးဖြင့် Auto-generate လုပ်မည်(ဥပမာ - YAN-101)
		String baseCode = request.getCity().substring(0, Math.min(3, request.getCity().length())).toUpperCase() + "-"
				+ (System.currentTimeMillis() % 1000);

		// က။ ရွေးချယ်ထားသော မြို့နယ်တစ်ခုချင်းစီအတွက် Specific Zone များ ခွဲသိမ်းမည်
		if (request.getTownships() != null && !request.getTownships().isEmpty()) {
			int index = 1;
			for (String township : request.getTownships()) {
				zoneRepository.findByCityAndTownship(request.getCity(), township).ifPresent(existing -> {
					throw new IllegalArgumentException(
							township + " is '" + existing.getZoneName() + "' already in the zone.");
				});

				String generatedCode = String.format("%s-%02d", baseCode, index++);

				// Mapper ကို အသုံးပြု၍ Entity တည်ဆောက်ခြင်း
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
