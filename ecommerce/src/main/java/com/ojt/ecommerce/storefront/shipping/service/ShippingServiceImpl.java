package com.ojt.ecommerce.storefront.shipping.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.checkout.dto.*;
import com.ojt.ecommerce.storefront.profile.repository.CustomerAddressRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5DeliveryZoneRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShippingRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {

    private final G5DeliveryZoneRepository deliveryZoneRepository;
    private final G5ShippingRateRepository shippingRateRepository;
    private final CustomerAddressRepository customerAddressRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryZoneResponse> getAvailableDeliveryZones() {
        List<DeliveryZone> zones = deliveryZoneRepository.findByStatus("ACTIVE");
        List<DeliveryZoneResponse> responses = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (DeliveryZone zone : zones) {
            Optional<ShippingRate> rateOpt = shippingRateRepository.findActiveRateForZone(zone.getZoneId(), now);
            if (rateOpt.isPresent()) {
                ShippingRate rate = rateOpt.get();
                responses.add(DeliveryZoneResponse.builder()
                        .zoneId(zone.getZoneId())
                        .zoneCode(zone.getZoneCode())
                        .zoneName(zone.getZoneName())
                        .city(zone.getCity())
                        .township(zone.getTownship())
                        .regionOrState(zone.getRegionOrState())
                        .shippingFee(rate.getFee())
                        .estimatedDays(rate.getEstimatedDays())
                        .build());
            }
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingQuoteResponse calculateQuoteForAddress(String city, String township) {
        LocalDateTime now = LocalDateTime.now();
        
        Optional<DeliveryZone> zoneOpt = deliveryZoneRepository.findByCityAndTownshipAndStatus(city, township, "ACTIVE");
        if (zoneOpt.isEmpty()) {
            zoneOpt = deliveryZoneRepository.findByCityAndStatusAndTownshipIsNull(city, "ACTIVE");
        }
        
        if (zoneOpt.isEmpty()) {
            throw new ResourceNotFoundException("No delivery zone available for the specified location");
        }
        
        DeliveryZone zone = zoneOpt.get();
        ShippingRate rate = shippingRateRepository.findActiveRateForZone(zone.getZoneId(), now)
                .orElseThrow(() -> new ResourceNotFoundException("No active shipping rate found for this zone"));
                
        return ShippingQuoteResponse.builder()
                .shippingRateId(rate.getShippingRateId())
                .zoneId(zone.getZoneId())
                .zoneCode(zone.getZoneCode())
                .zoneName(zone.getZoneName())
                .city(zone.getCity())
                .township(zone.getTownship())
                .shippingFee(rate.getFee())
                .estimatedDays(rate.getEstimatedDays())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingQuoteResponse getShippingQuote(Long customerId, Long addressId) {
        CustomerAddress address = customerAddressRepository.findByAddressIdAndCustomerCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return calculateQuoteForAddress(address.getCity(), address.getTownship());
    }

    @Override
    @Transactional(readOnly = true)
    public ShippingQuoteResponse getCustomShippingQuote(CustomShippingQuoteRequest request) {
        if (request.getCity() == null) {
            throw new InvalidRequestException("City is required");
        }
        return calculateQuoteForAddress(request.getCity(), request.getTownship());
    }
}
