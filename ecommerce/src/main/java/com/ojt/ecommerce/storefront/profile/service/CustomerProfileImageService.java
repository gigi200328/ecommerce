package com.ojt.ecommerce.storefront.profile.service;

import com.ojt.ecommerce.storefront.profile.dto.CustomerProfileImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CustomerProfileImageService {
    CustomerProfileImageResponse uploadProfileImage(Long customerId, MultipartFile file);
    CustomerProfileImageResponse updateProfileImage(Long customerId, MultipartFile file);
    void deleteProfileImage(Long customerId);
    String getProfileImageUrl(Long customerId);
    CustomerProfileImageResponse updateProfileAvatar(Long customerId, String avatarUrl);
}
