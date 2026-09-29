package com.ojt.ecommerce.storefront.shipping.service;
import com.ojt.ecommerce.storefront.checkout.dto.*;
import java.util.List;
public interface ShippingService {
    List<DeliveryZoneResponse> getAvailableDeliveryZones();
    ShippingQuoteResponse getShippingQuote(Long customerId, Long addressId);
    ShippingQuoteResponse getCustomShippingQuote(CustomShippingQuoteRequest request);
    ShippingQuoteResponse calculateQuoteForAddress(String city, String township);
}