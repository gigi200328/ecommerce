package com.ojt.ecommerce.storefront.shipping.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.CustomerAddress;
import com.ojt.ecommerce.entity.DeliveryZone;
import com.ojt.ecommerce.entity.ShippingRate;
import com.ojt.ecommerce.storefront.checkout.dto.CustomShippingQuoteRequest;
import com.ojt.ecommerce.storefront.checkout.dto.DeliveryZoneResponse;
import com.ojt.ecommerce.storefront.checkout.dto.ShippingQuoteResponse;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.profile.repository.CustomerAddressRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5DeliveryZoneRepository;
import com.ojt.ecommerce.storefront.shipping.repository.G5ShippingRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private G5DeliveryZoneRepository deliveryZoneRepository;

    @Mock
    private G5ShippingRateRepository shippingRateRepository;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @InjectMocks
    private ShippingServiceImpl shippingService;

    private DeliveryZone zoneYangon;
    private DeliveryZone zoneMandalay;
    private ShippingRate rateYangon;
    private ShippingRate rateMandalay;

    @BeforeEach
    void setUp() {
        zoneYangon = DeliveryZone.builder()
                .zoneId(1L)
                .zoneCode("ZONE-YGN-01")
                .zoneName("Yangon Downtown")
                .city("Yangon")
                .township("Kyauktada")
                .regionOrState("Yangon Region")
                .status("ACTIVE")
                .build();

        zoneMandalay = DeliveryZone.builder()
                .zoneId(2L)
                .zoneCode("ZONE-MDY-01")
                .zoneName("Mandalay Central")
                .city("Mandalay")
                .township(null)
                .regionOrState("Mandalay Region")
                .status("ACTIVE")
                .build();

        rateYangon = ShippingRate.builder()
                .shippingRateId(101L)
                .zone(zoneYangon)
                .fee(new BigDecimal("2500.00"))
                .estimatedDays(2)
                .status("ACTIVE")
                .build();

        rateMandalay = ShippingRate.builder()
                .shippingRateId(102L)
                .zone(zoneMandalay)
                .fee(new BigDecimal("4000.00"))
                .estimatedDays(4)
                .status("ACTIVE")
                .build();
    }

    // =========================================================================
    // 1. getAvailableDeliveryZones Tests
    // =========================================================================

    @Test
    @DisplayName("Should return available zones with active shipping fees")
    void getAvailableDeliveryZones_Success() {
        when(deliveryZoneRepository.findByStatus("ACTIVE")).thenReturn(List.of(zoneYangon, zoneMandalay));
        when(shippingRateRepository.findActiveRateForZone(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateYangon));
        when(shippingRateRepository.findActiveRateForZone(eq(2L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateMandalay));

        List<DeliveryZoneResponse> responses = shippingService.getAvailableDeliveryZones();

        assertNotNull(responses);
        assertEquals(2, responses.size());

        DeliveryZoneResponse r1 = responses.get(0);
        assertEquals(1L, r1.getZoneId());
        assertEquals("Yangon", r1.getCity());
        assertEquals("Kyauktada", r1.getTownship());
        assertEquals(new BigDecimal("2500.00"), r1.getShippingFee());
        assertEquals(2, r1.getEstimatedDays());

        DeliveryZoneResponse r2 = responses.get(1);
        assertEquals(2L, r2.getZoneId());
        assertEquals("Mandalay", r2.getCity());
        assertNull(r2.getTownship());
        assertEquals(new BigDecimal("4000.00"), r2.getShippingFee());
        assertEquals(4, r2.getEstimatedDays());
    }

    @Test
    @DisplayName("Should skip zones that have no active shipping rate")
    void getAvailableDeliveryZones_SkipsZonesWithoutRates() {
        when(deliveryZoneRepository.findByStatus("ACTIVE")).thenReturn(List.of(zoneYangon, zoneMandalay));
        when(shippingRateRepository.findActiveRateForZone(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateYangon));
        // zoneMandalay has no active rate
        when(shippingRateRepository.findActiveRateForZone(eq(2L), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        List<DeliveryZoneResponse> responses = shippingService.getAvailableDeliveryZones();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getZoneId());
    }

    @Test
    @DisplayName("Should return empty list when no active delivery zones exist")
    void getAvailableDeliveryZones_EmptyList() {
        when(deliveryZoneRepository.findByStatus("ACTIVE")).thenReturn(Collections.emptyList());

        List<DeliveryZoneResponse> responses = shippingService.getAvailableDeliveryZones();

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
        verifyNoInteractions(shippingRateRepository);
    }

    // =========================================================================
    // 2. calculateQuoteForAddress Tests (Exact Match vs Fallback)
    // =========================================================================

    @Test
    @DisplayName("Should calculate shipping quote via exact city and township match")
    void calculateQuoteForAddress_ExactMatch_Success() {
        when(deliveryZoneRepository.findByCityAndTownshipAndStatus("Yangon", "Kyauktada", "ACTIVE"))
                .thenReturn(Optional.of(zoneYangon));
        when(shippingRateRepository.findActiveRateForZone(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateYangon));

        ShippingQuoteResponse quote = shippingService.calculateQuoteForAddress("Yangon", "Kyauktada");

        assertNotNull(quote);
        assertEquals(101L, quote.getShippingRateId());
        assertEquals(1L, quote.getZoneId());
        assertEquals("Yangon", quote.getCity());
        assertEquals("Kyauktada", quote.getTownship());
        assertEquals(new BigDecimal("2500.00"), quote.getShippingFee());
        assertEquals(2, quote.getEstimatedDays());

        // Verify city-only fallback was NOT queried because exact match succeeded
        verify(deliveryZoneRepository, never()).findByCityAndStatusAndTownshipIsNull(anyString(), anyString());
    }

    @Test
    @DisplayName("Should fallback to city-wide zone when township specific zone is not found")
    void calculateQuoteForAddress_FallbackToCityOnly_Success() {
        // Exact match with township returns empty
        when(deliveryZoneRepository.findByCityAndTownshipAndStatus("Mandalay", "Chanayethazan", "ACTIVE"))
                .thenReturn(Optional.empty());
        // Fallback to city with null township returns zoneMandalay
        when(deliveryZoneRepository.findByCityAndStatusAndTownshipIsNull("Mandalay", "ACTIVE"))
                .thenReturn(Optional.of(zoneMandalay));
        when(shippingRateRepository.findActiveRateForZone(eq(2L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateMandalay));

        ShippingQuoteResponse quote = shippingService.calculateQuoteForAddress("Mandalay", "Chanayethazan");

        assertNotNull(quote);
        assertEquals(102L, quote.getShippingRateId());
        assertEquals(2L, quote.getZoneId());
        assertEquals("Mandalay", quote.getCity());
        assertEquals(new BigDecimal("4000.00"), quote.getShippingFee());
        assertEquals(4, quote.getEstimatedDays());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when no delivery zone matches city and township")
    void calculateQuoteForAddress_ThrowsNotFound_WhenNoZone() {
        when(deliveryZoneRepository.findByCityAndTownshipAndStatus("Taunggyi", "Aye Thar Yar", "ACTIVE"))
                .thenReturn(Optional.empty());
        when(deliveryZoneRepository.findByCityAndStatusAndTownshipIsNull("Taunggyi", "ACTIVE"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> shippingService.calculateQuoteForAddress("Taunggyi", "Aye Thar Yar"));

        assertEquals("No delivery zone available for the specified location", ex.getMessage());
        verifyNoInteractions(shippingRateRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when delivery zone exists but has no active rate")
    void calculateQuoteForAddress_ThrowsNotFound_WhenNoActiveRate() {
        when(deliveryZoneRepository.findByCityAndTownshipAndStatus("Yangon", "Kyauktada", "ACTIVE"))
                .thenReturn(Optional.of(zoneYangon));
        when(shippingRateRepository.findActiveRateForZone(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> shippingService.calculateQuoteForAddress("Yangon", "Kyauktada"));

        assertEquals("No active shipping rate found for this zone", ex.getMessage());
    }

    // =========================================================================
    // 3. getShippingQuote Tests (Customer Address & IDOR Protection)
    // =========================================================================

    @Test
    @DisplayName("Should return shipping quote for customer address successfully")
    void getShippingQuote_Success() {
        Customer customer = Customer.builder().customerId(10L).build();
        CustomerAddress address = CustomerAddress.builder()
                .addressId(50L)
                .customer(customer)
                .city("Yangon")
                .township("Kyauktada")
                .build();

        when(customerAddressRepository.findByAddressIdAndCustomerCustomerId(50L, 10L))
                .thenReturn(Optional.of(address));
        when(deliveryZoneRepository.findByCityAndTownshipAndStatus("Yangon", "Kyauktada", "ACTIVE"))
                .thenReturn(Optional.of(zoneYangon));
        when(shippingRateRepository.findActiveRateForZone(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateYangon));

        ShippingQuoteResponse quote = shippingService.getShippingQuote(10L, 50L);

        assertNotNull(quote);
        assertEquals(new BigDecimal("2500.00"), quote.getShippingFee());
        verify(customerAddressRepository, times(1)).findByAddressIdAndCustomerCustomerId(50L, 10L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when address does not exist or does not belong to customer (IDOR)")
    void getShippingQuote_ThrowsNotFound_WhenAddressMismatch() {
        when(customerAddressRepository.findByAddressIdAndCustomerCustomerId(99L, 10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> shippingService.getShippingQuote(10L, 99L));

        assertEquals("Address not found", ex.getMessage());
        verifyNoInteractions(deliveryZoneRepository);
        verifyNoInteractions(shippingRateRepository);
    }

    // =========================================================================
    // 4. getCustomShippingQuote Tests
    // =========================================================================

    @Test
    @DisplayName("Should return quote for custom address request")
    void getCustomShippingQuote_Success() {
        CustomShippingQuoteRequest request = new CustomShippingQuoteRequest();
        request.setCity("Yangon");
        request.setTownship("Kyauktada");

        when(deliveryZoneRepository.findByCityAndTownshipAndStatus("Yangon", "Kyauktada", "ACTIVE"))
                .thenReturn(Optional.of(zoneYangon));
        when(shippingRateRepository.findActiveRateForZone(eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(rateYangon));

        ShippingQuoteResponse quote = shippingService.getCustomShippingQuote(request);

        assertNotNull(quote);
        assertEquals(new BigDecimal("2500.00"), quote.getShippingFee());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when city is null in custom request")
    void getCustomShippingQuote_ThrowsException_WhenCityNull() {
        CustomShippingQuoteRequest request = new CustomShippingQuoteRequest();
        request.setCity(null);
        request.setTownship("Kyauktada");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> shippingService.getCustomShippingQuote(request));

        assertEquals("City is required", ex.getMessage());
        verifyNoInteractions(deliveryZoneRepository);
    }
}
