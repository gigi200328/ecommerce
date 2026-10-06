package com.ojt.ecommerce.storefront.profile.controller;

import com.ojt.ecommerce.storefront.profile.dto.CustomerProfileImageResponse;
import com.ojt.ecommerce.storefront.profile.service.CustomerProfileImageService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront/v1/profile/image")
@RequiredArgsConstructor
public class CustomerProfileImageController {

    private final CustomerProfileImageService profileImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerProfileImageResponse> uploadProfileImage(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        CustomerProfileImageResponse response = profileImageService.uploadProfileImage(principal.getCustomerId(), file);
        return ResponseEntity.ok(response);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CustomerProfileImageResponse> updateProfileImage(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        CustomerProfileImageResponse response = profileImageService.updateProfileImage(principal.getCustomerId(), file);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteProfileImage(
            @AuthenticationPrincipal CustomerPrincipal principal) {
        profileImageService.deleteProfileImage(principal.getCustomerId());
        return ResponseEntity.ok(Map.of("message", "Profile image deleted successfully"));
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getProfileImage(
            @AuthenticationPrincipal CustomerPrincipal principal) {
        String imageUrl = profileImageService.getProfileImageUrl(principal.getCustomerId());
        return ResponseEntity.ok(Map.of("profileImageUrl", imageUrl != null ? imageUrl : ""));
    }

    @RequestMapping(value = "/avatar", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<CustomerProfileImageResponse> updateProfileAvatar(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestBody Map<String, String> request) {
        String avatarUrl = request.get("avatarUrl");
        CustomerProfileImageResponse response = profileImageService.updateProfileAvatar(principal.getCustomerId(), avatarUrl);
        return ResponseEntity.ok(response);
    }
}
