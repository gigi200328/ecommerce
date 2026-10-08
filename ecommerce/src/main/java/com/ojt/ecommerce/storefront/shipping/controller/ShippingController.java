package com.ojt.ecommerce.storefront.shipping.controller;

import com.ojt.ecommerce.storefront.checkout.dto.CustomShippingQuoteRequest;
import com.ojt.ecommerce.storefront.checkout.dto.DeliveryZoneResponse;
import com.ojt.ecommerce.storefront.checkout.dto.ShippingQuoteResponse;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;

import com.ojt.ecommerce.storefront.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storefront/v1")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @GetMapping("/delivery-zones/available")
    public ResponseEntity<List<DeliveryZoneResponse>> getAvailableDeliveryZones() {
        return ResponseEntity.ok(shippingService.getAvailableDeliveryZones());
    }

    @GetMapping("/shipping/quote")
    public ResponseEntity<ShippingQuoteResponse> getShippingQuote(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestParam Long addressId) {
        return ResponseEntity.ok(shippingService.getShippingQuote(principal.getCustomerId(), addressId));
    }

    @PostMapping("/shipping/quote/custom")
    public ResponseEntity<ShippingQuoteResponse> getCustomShippingQuote(
            @RequestBody CustomShippingQuoteRequest request) {
        return ResponseEntity.ok(shippingService.getCustomShippingQuote(request));
    }
}
