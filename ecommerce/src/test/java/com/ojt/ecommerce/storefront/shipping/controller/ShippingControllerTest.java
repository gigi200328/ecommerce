package com.ojt.ecommerce.storefront.shipping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.storefront.checkout.dto.CustomShippingQuoteRequest;
import com.ojt.ecommerce.storefront.checkout.dto.DeliveryZoneResponse;
import com.ojt.ecommerce.storefront.checkout.dto.ShippingQuoteResponse;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import com.ojt.ecommerce.storefront.shipping.service.ShippingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ShippingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ShippingService shippingService;

    @InjectMocks
    private ShippingController shippingController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DeliveryZoneResponse zoneResponse;
    private ShippingQuoteResponse quoteResponse;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver customerPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(CustomerPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new CustomerPrincipal(1L, "customer@example.com");
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(shippingController)
                .setCustomArgumentResolvers(customerPrincipalResolver)
                .build();

        zoneResponse = DeliveryZoneResponse.builder()
                .zoneId(1L)
                .zoneCode("ZONE-YGN-01")
                .zoneName("Yangon Downtown")
                .city("Yangon")
                .township("Kyauktada")
                .shippingFee(new BigDecimal("2500.00"))
                .estimatedDays(2)
                .build();

        quoteResponse = ShippingQuoteResponse.builder()
                .shippingRateId(101L)
                .zoneId(1L)
                .city("Yangon")
                .township("Kyauktada")
                .shippingFee(new BigDecimal("2500.00"))
                .estimatedDays(2)
                .build();
    }

    @Test
    @DisplayName("GET /api/storefront/v1/delivery-zones/available - Should return list of active delivery zones")
    void getAvailableDeliveryZones_Success() throws Exception {
        when(shippingService.getAvailableDeliveryZones()).thenReturn(List.of(zoneResponse));

        mockMvc.perform(get("/api/storefront/v1/delivery-zones/available"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].zoneId").value(1L))
                .andExpect(jsonPath("$[0].city").value("Yangon"))
                .andExpect(jsonPath("$[0].township").value("Kyauktada"))
                .andExpect(jsonPath("$[0].shippingFee").value(2500.00))
                .andExpect(jsonPath("$[0].estimatedDays").value(2));

        verify(shippingService, times(1)).getAvailableDeliveryZones();
    }

    @Test
    @DisplayName("GET /api/storefront/v1/shipping/quote - Should return shipping quote for saved address")
    void getShippingQuote_Success() throws Exception {
        when(shippingService.getShippingQuote(1L, 50L)).thenReturn(quoteResponse);

        mockMvc.perform(get("/api/storefront/v1/shipping/quote")
                        .param("addressId", "50"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shippingRateId").value(101L))
                .andExpect(jsonPath("$.zoneId").value(1L))
                .andExpect(jsonPath("$.city").value("Yangon"))
                .andExpect(jsonPath("$.shippingFee").value(2500.00))
                .andExpect(jsonPath("$.estimatedDays").value(2));

        verify(shippingService, times(1)).getShippingQuote(1L, 50L);
    }

    @Test
    @DisplayName("POST /api/storefront/v1/shipping/quote/custom - Should return quote for custom address request")
    void getCustomShippingQuote_Success() throws Exception {
        CustomShippingQuoteRequest request = new CustomShippingQuoteRequest();
        request.setCity("Yangon");
        request.setTownship("Kyauktada");

        when(shippingService.getCustomShippingQuote(any(CustomShippingQuoteRequest.class))).thenReturn(quoteResponse);

        mockMvc.perform(post("/api/storefront/v1/shipping/quote/custom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shippingRateId").value(101L))
                .andExpect(jsonPath("$.shippingFee").value(2500.00));

        verify(shippingService, times(1)).getCustomShippingQuote(any(CustomShippingQuoteRequest.class));
    }
}
