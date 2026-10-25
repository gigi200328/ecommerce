package com.ojt.ecommerce.storefront.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressResponse;
import com.ojt.ecommerce.storefront.profile.service.CustomerAddressService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerAddressService addressService;

    @InjectMocks
    private CustomerAddressController addressController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CustomerAddressResponse sampleResponse;
    private CustomerAddressRequest sampleRequest;

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
                return new CustomerPrincipal(1L, "john@example.com");
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(addressController)
                .setCustomArgumentResolvers(customerPrincipalResolver)
                .build();

        sampleResponse = new CustomerAddressResponse();
        sampleResponse.setAddressId(10L);
        sampleResponse.setLabel("Home");
        sampleResponse.setRecipientName("John Doe");
        sampleResponse.setPhoneNumber("09123456789");
        sampleResponse.setAddressLine1("No. 12, Main Road");
        sampleResponse.setCity("Yangon");
        sampleResponse.setTownship("Kyauktada");
        sampleResponse.setIsDefault(true);

        sampleRequest = new CustomerAddressRequest();
        sampleRequest.setLabel("Home");
        sampleRequest.setRecipientName("John Doe");
        sampleRequest.setPhoneNumber("09123456789");
        sampleRequest.setAddressLine1("No. 12, Main Road");
        sampleRequest.setCity("Yangon");
        sampleRequest.setTownship("Kyauktada");
        sampleRequest.setIsDefault(true);
    }

    @Test
    @DisplayName("GET /api/storefront/v1/profile/addresses - Should return 200 OK with address list")
    void getAddresses_Success() throws Exception {
        when(addressService.getAddresses(1L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/storefront/v1/profile/addresses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].addressId").value(10L))
                .andExpect(jsonPath("$[0].recipientName").value("John Doe"))
                .andExpect(jsonPath("$[0].city").value("Yangon"))
                .andExpect(jsonPath("$[0].isDefault").value(true));

        verify(addressService, times(1)).getAddresses(1L);
    }

    @Test
    @DisplayName("POST /api/storefront/v1/profile/addresses - Should return 200 OK with created address")
    void createAddress_Success() throws Exception {
        when(addressService.createAddress(eq(1L), any(CustomerAddressRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/storefront/v1/profile/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(10L))
                .andExpect(jsonPath("$.recipientName").value("John Doe"))
                .andExpect(jsonPath("$.city").value("Yangon"));

        verify(addressService, times(1)).createAddress(eq(1L), any(CustomerAddressRequest.class));
    }

    @Test
    @DisplayName("PUT /api/storefront/v1/profile/addresses/{id} - Should return 200 OK with updated address")
    void updateAddress_Success() throws Exception {
        when(addressService.updateAddress(eq(1L), eq(10L), any(CustomerAddressRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/storefront/v1/profile/addresses/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(10L))
                .andExpect(jsonPath("$.recipientName").value("John Doe"));

        verify(addressService, times(1)).updateAddress(eq(1L), eq(10L), any(CustomerAddressRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/storefront/v1/profile/addresses/{id} - Should return 204 No Content")
    void deleteAddress_Success() throws Exception {
        doNothing().when(addressService).deleteAddress(1L, 10L);

        mockMvc.perform(delete("/api/storefront/v1/profile/addresses/10"))
                .andExpect(status().isNoContent());

        verify(addressService, times(1)).deleteAddress(1L, 10L);
    }

    @Test
    @DisplayName("PATCH /api/storefront/v1/profile/addresses/{id}/default - Should return 200 OK with updated default")
    void setDefaultAddressPatch_Success() throws Exception {
        when(addressService.setDefaultAddress(1L, 10L)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/storefront/v1/profile/addresses/10/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(10L))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(addressService, times(1)).setDefaultAddress(1L, 10L);
    }

    @Test
    @DisplayName("PUT /api/storefront/v1/profile/addresses/{id}/default - Should return 200 OK with updated default")
    void setDefaultAddressPut_Success() throws Exception {
        when(addressService.setDefaultAddress(1L, 10L)).thenReturn(sampleResponse);

        mockMvc.perform(put("/api/storefront/v1/profile/addresses/10/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(10L))
                .andExpect(jsonPath("$.isDefault").value(true));

        verify(addressService, times(1)).setDefaultAddress(1L, 10L);
    }
}
