package com.ojt.ecommerce.backofficeinventory.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.ojt.ecommerce.backofficeinventory.dto.CreateZoneGroupRequest;
import com.ojt.ecommerce.backofficeinventory.dto.ShipmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.UpdateZoneRateRequest;
import com.ojt.ecommerce.backofficeinventory.dto.ZoneGroupResponse;
import com.ojt.ecommerce.entity.DeliveryZone;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.Shipment;
import com.ojt.ecommerce.entity.ShippingRate;

@Component
public class ShipmentMapper {
	public Shipment toEntity(ShipmentRequestDto dto, Order order) {
		return Shipment.builder().order(order).courierName(dto.getCourierName()).trackingNumber(dto.getTrackingNumber())
				.shipmentStatus("SHIPPED").shippedAt(LocalDateTime.now()).createdAt(LocalDateTime.now()).build();
	}

	public void updateEntity(Shipment existingShipment, ShipmentRequestDto dto) {
		existingShipment.setCourierName((dto.getCourierName()));
		existingShipment.setTrackingNumber(dto.getTrackingNumber());
		existingShipment.setShippedAt(LocalDateTime.now());
	}

	public DeliveryZone toDeliveryZoneEntity(CreateZoneGroupRequest request, String generatedCode, String township,
			LocalDateTime now) {
		DeliveryZone zone = new DeliveryZone();
		zone.setZoneName(request.getZoneName());
		zone.setZoneCode(generatedCode);
		zone.setRegionOrState(request.getRegionOrState());
		zone.setCity(request.getCity());
		zone.setTownship(township);
		zone.setStatus("ACTIVE");
		zone.setCreatedAt(now);
		zone.setModifiedAt(now);
		return zone;
	}

	// ၂။ ShippingRate Entity အသစ် တည်ဆောက်ခြင်း
	public ShippingRate toShippingRateEntity(DeliveryZone zone, BigDecimal fee, Integer estimatedDays,
			LocalDateTime now) {
		ShippingRate rate = new ShippingRate();
		rate.setZone(zone);
		rate.setFee(fee);
		rate.setEstimatedDays(estimatedDays);
		rate.setStatus("ACTIVE");
		rate.setEffectiveFrom(now);
		rate.setEffectiveTo(null);
		rate.setCreatedAt(now);
		rate.setModifiedAt(now);
		return rate;
	}

	// ၃။ DeliveryZone နှင့် ShippingRate မှ ZoneGroupResponse DTO သို့ ပြောင်းခြင်း
	public ZoneGroupResponse toInitialZoneGroupResponse(DeliveryZone zone, Optional<ShippingRate> rateOpt) {
		BigDecimal fee = rateOpt.map(ShippingRate::getFee).orElse(BigDecimal.ZERO);
		Integer estDays = rateOpt.map(ShippingRate::getEstimatedDays).orElse(null);

		return ZoneGroupResponse.builder().zoneName(zone.getZoneName()).zoneCode(zone.getZoneCode())
				.regionOrState(zone.getRegionOrState()).city(zone.getCity()).townships(new ArrayList<>())
				.zoneIds(new ArrayList<>()).isCityDefault(zone.getTownship() == null).shippingFee(fee)
				.estimatedDays(estDays).status(zone.getStatus()).build();
	}

	// ၄။ ရှိပြီးသား DeliveryZone Entity ကို Update Request ဖြင့် ပြင်ဆင်ခြင်း
	public void updateZoneEntity(DeliveryZone zone, UpdateZoneRateRequest request, LocalDateTime now) {
		if (request.getZoneName() != null && !request.getZoneName().isBlank()) {
			zone.setZoneName(request.getZoneName());
		}
		if (request.getStatus() != null && !request.getStatus().isBlank()) {
			zone.setStatus(request.getStatus().toUpperCase());
		}
		zone.setModifiedAt(now);
	}

	// ၅။ Rate အဟောင်းကို Inactive ပြောင်းခြင်း
	public void deactivateRateEntity(ShippingRate oldRate, LocalDateTime now) {
		oldRate.setEffectiveTo(now);
		oldRate.setStatus("INACTIVE");
		oldRate.setModifiedAt(now);
	}

}