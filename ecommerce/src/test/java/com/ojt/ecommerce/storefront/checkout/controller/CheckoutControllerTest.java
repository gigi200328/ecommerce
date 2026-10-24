package com.ojt.ecommerce.storefront.checkout.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.storefront.checkout.dto.CreateOrderRequest;
import com.ojt.ecommerce.storefront.checkout.dto.OrderResponse;
import com.ojt.ecommerce.storefront.checkout.service.CheckoutService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CheckoutService checkoutService;

    @InjectMocks
    private CheckoutController checkoutController;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        mockMvc = MockMvcBuilders.standaloneSetup(checkoutController)
                .setCustomArgumentResolvers(customerPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/storefront/v1/orders - Should create order successfully and return OrderResponse")
    void createOrder_Success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSavedAddressId(50L);

        OrderResponse response = OrderResponse.builder()
                .orderId(101L)
                .orderNo("ORD-260101-ABCDEF")
                .subtotalAmount(new BigDecimal("30000.00"))
                .shippingFee(new BigDecimal("2500.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("32500.00"))
                .orderStatus("PENDING")
                .paymentStatus("PENDING")
                .createdAt("2026-10-24T10:00:00")
                .build();

        when(checkoutService.createOrder(eq(1L), any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/storefront/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.orderId").value(101L))
                .andExpect(jsonPath("$.orderNo").value("ORD-260101-ABCDEF"))
                .andExpect(jsonPath("$.subtotalAmount").value(30000.00))
                .andExpect(jsonPath("$.shippingFee").value(2500.00))
                .andExpect(jsonPath("$.totalAmount").value(32500.00))
                .andExpect(jsonPath("$.orderStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"));

        verify(checkoutService, times(1)).createOrder(eq(1L), any(CreateOrderRequest.class));
    }
}
