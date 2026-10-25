package com.ojt.ecommerce.storefront.shipping.controller;

import com.ojt.ecommerce.storefront.checkout.dto.CustomShippingQuoteRequest;
import com.ojt.ecommerce.storefront.checkout.dto.DeliveryZoneResponse;
import com.ojt.ecommerce.storefront.checkout.dto.ShippingQuoteResponse;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import com.ojt.ecommerce.storefront.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingControllerTest {

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingController shippingController;

    private CustomerPrincipal principal;
    private DeliveryZoneResponse zoneResponse;
    private ShippingQuoteResponse quoteResponse;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");

        zoneResponse = DeliveryZoneResponse.builder()
                .zoneId(1L)
                .zoneName("Yangon Metro")
                .city("Yangon")
                .regionOrState("Yangon")
                .estimatedDays(2)
                .build();

        quoteResponse = ShippingQuoteResponse.builder()
                .zoneId(1L)
                .zoneName("Yangon Metro")
                .shippingFee(new BigDecimal("3000.00"))
                .estimatedDays(2)
                .build();
    }

    // =========================================================================
    // 1. getAvailableDeliveryZones Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with list of active delivery zones")
    void getAvailableDeliveryZones_Success() {
        when(shippingService.getAvailableDeliveryZones()).thenReturn(List.of(zoneResponse));

        ResponseEntity<List<DeliveryZoneResponse>> response = shippingController.getAvailableDeliveryZones();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Yangon Metro", response.getBody().get(0).getZoneName());

        verify(shippingService, times(1)).getAvailableDeliveryZones();
    }

    // =========================================================================
    // 2. getShippingQuote Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with shipping quote for customer address")
    void getShippingQuote_Success() {
        when(shippingService.getShippingQuote(1L, 501L)).thenReturn(quoteResponse);

        ResponseEntity<ShippingQuoteResponse> response = shippingController.getShippingQuote(principal, 501L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("3000.00"), response.getBody().getShippingFee());
        assertEquals(2, response.getBody().getEstimatedDays());

        verify(shippingService, times(1)).getShippingQuote(1L, 501L);
    }

    @Test
    @DisplayName("Should propagate ResourceNotFoundException when address is not found")
    void getShippingQuote_NotFound() {
        when(shippingService.getShippingQuote(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Customer address not found"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> shippingController.getShippingQuote(principal, 999L));

        assertEquals("Customer address not found", ex.getMessage());
        verify(shippingService, times(1)).getShippingQuote(1L, 999L);
    }

    // =========================================================================
    // 3. getCustomShippingQuote Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with custom quote when city and township match a zone")
    void getCustomShippingQuote_Success() {
        CustomShippingQuoteRequest request = new CustomShippingQuoteRequest();
        request.setCity("Yangon");
        request.setTownship("Kyauktada");

        when(shippingService.getCustomShippingQuote(request)).thenReturn(quoteResponse);

        ResponseEntity<ShippingQuoteResponse> response = shippingController.getCustomShippingQuote(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Yangon Metro", response.getBody().getZoneName());

        verify(shippingService, times(1)).getCustomShippingQuote(request);
    }
}
