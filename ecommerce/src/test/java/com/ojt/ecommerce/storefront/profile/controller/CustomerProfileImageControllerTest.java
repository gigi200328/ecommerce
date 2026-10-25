package com.ojt.ecommerce.storefront.profile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.storefront.profile.dto.CustomerProfileImageResponse;
import com.ojt.ecommerce.storefront.profile.service.CustomerProfileImageService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileImageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerProfileImageService profileImageService;

    @InjectMocks
    private CustomerProfileImageController profileImageController;

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
                return new CustomerPrincipal(1L, "john@example.com");
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(profileImageController)
                .setCustomArgumentResolvers(customerPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/storefront/v1/profile/image - Should upload image successfully")
    void uploadProfileImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "test image content".getBytes()
        );

        CustomerProfileImageResponse response = CustomerProfileImageResponse.builder()
                .profileImageUrl("/uploads/profile-images/uuid-123.png")
                .message("Profile image uploaded successfully")
                .build();

        when(profileImageService.uploadProfileImage(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/storefront/v1/profile/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("/uploads/profile-images/uuid-123.png"))
                .andExpect(jsonPath("$.message").value("Profile image uploaded successfully"));

        verify(profileImageService, times(1)).uploadProfileImage(eq(1L), any());
    }

    @Test
    @DisplayName("PUT /api/storefront/v1/profile/image - Should update image successfully")
    void updateProfileImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "new-avatar.png", "image/png", "new test content".getBytes()
        );

        CustomerProfileImageResponse response = CustomerProfileImageResponse.builder()
                .profileImageUrl("/uploads/profile-images/uuid-456.png")
                .message("Profile image updated successfully")
                .build();

        when(profileImageService.updateProfileImage(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/storefront/v1/profile/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("/uploads/profile-images/uuid-456.png"))
                .andExpect(jsonPath("$.message").value("Profile image updated successfully"));

        verify(profileImageService, times(1)).updateProfileImage(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/storefront/v1/profile/image - Should delete profile image")
    void deleteProfileImage_Success() throws Exception {
        doNothing().when(profileImageService).deleteProfileImage(1L);

        mockMvc.perform(delete("/api/storefront/v1/profile/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile image deleted successfully"));

        verify(profileImageService, times(1)).deleteProfileImage(1L);
    }

    @Test
    @DisplayName("GET /api/storefront/v1/profile/image - Should return profile image URL")
    void getProfileImage_Success() throws Exception {
        when(profileImageService.getProfileImageUrl(1L)).thenReturn("/uploads/profile-images/uuid-123.png");

        mockMvc.perform(get("/api/storefront/v1/profile/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("/uploads/profile-images/uuid-123.png"));

        verify(profileImageService, times(1)).getProfileImageUrl(1L);
    }

    @Test
    @DisplayName("GET /api/storefront/v1/profile/image - Should return empty string when URL is null")
    void getProfileImage_NullUrl_ReturnsEmptyString() throws Exception {
        when(profileImageService.getProfileImageUrl(1L)).thenReturn(null);

        mockMvc.perform(get("/api/storefront/v1/profile/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(""));
    }

    @Test
    @DisplayName("POST /api/storefront/v1/profile/image/avatar - Should update profile avatar URL")
    void updateProfileAvatar_Post_Success() throws Exception {
        Map<String, String> request = Map.of("avatarUrl", "https://api.dicebear.com/avatar.svg");
        CustomerProfileImageResponse response = CustomerProfileImageResponse.builder()
                .profileImageUrl("https://api.dicebear.com/avatar.svg")
                .message("Profile avatar updated successfully")
                .build();

        when(profileImageService.updateProfileAvatar(1L, "https://api.dicebear.com/avatar.svg")).thenReturn(response);

        mockMvc.perform(post("/api/storefront/v1/profile/image/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("https://api.dicebear.com/avatar.svg"))
                .andExpect(jsonPath("$.message").value("Profile avatar updated successfully"));

        verify(profileImageService, times(1)).updateProfileAvatar(1L, "https://api.dicebear.com/avatar.svg");
    }

    @Test
    @DisplayName("PUT /api/storefront/v1/profile/image/avatar - Should update profile avatar URL via PUT")
    void updateProfileAvatar_Put_Success() throws Exception {
        Map<String, String> request = Map.of("avatarUrl", "https://api.dicebear.com/avatar.svg");
        CustomerProfileImageResponse response = CustomerProfileImageResponse.builder()
                .profileImageUrl("https://api.dicebear.com/avatar.svg")
                .message("Profile avatar updated successfully")
                .build();

        when(profileImageService.updateProfileAvatar(1L, "https://api.dicebear.com/avatar.svg")).thenReturn(response);

        mockMvc.perform(put("/api/storefront/v1/profile/image/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("https://api.dicebear.com/avatar.svg"));

        verify(profileImageService, times(1)).updateProfileAvatar(1L, "https://api.dicebear.com/avatar.svg");
    }
}
