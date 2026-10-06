package com.ojt.ecommerce.storefront.profile.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.profile.dto.CustomerProfileImageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileImageServiceImpl implements CustomerProfileImageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String PROFILE_IMAGE_SUBDIR = "profile-images";
    private static final String URL_PREFIX = "/uploads/" + PROFILE_IMAGE_SUBDIR + "/";

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private final CustomerRepository customerRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public CustomerProfileImageResponse uploadProfileImage(Long customerId, MultipartFile file) {
        return processAndSaveImage(customerId, file, "Profile image uploaded successfully");
    }

    @Override
    @Transactional
    public CustomerProfileImageResponse updateProfileImage(Long customerId, MultipartFile file) {
        return processAndSaveImage(customerId, file, "Profile image updated successfully");
    }

    @Override
    @Transactional
    public void deleteProfileImage(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        String oldImageUrl = customer.getProfileImageUrl();
        if (oldImageUrl != null) {
            deletePhysicalFile(oldImageUrl);
        }

        customer.setProfileImageUrl(null);
        customer.setModifiedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public String getProfileImageUrl(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        return customer.getProfileImageUrl();
    }

    @Override
    @Transactional
    public CustomerProfileImageResponse updateProfileAvatar(Long customerId, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new InvalidRequestException("Avatar URL is required");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        // Delete old uploaded image file if present
        String oldImageUrl = customer.getProfileImageUrl();
        if (oldImageUrl != null) {
            deletePhysicalFile(oldImageUrl);
        }

        customer.setProfileImageUrl(avatarUrl.trim());
        customer.setModifiedAt(LocalDateTime.now());
        customerRepository.save(customer);

        return CustomerProfileImageResponse.builder()
                .profileImageUrl(avatarUrl.trim())
                .message("Profile avatar updated successfully")
                .build();
    }

    private CustomerProfileImageResponse processAndSaveImage(Long customerId, MultipartFile file, String successMessage) {
        validateFile(file);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        try {
            Path directory = getStorageDirectory();
            Files.createDirectories(directory);

            String extension = extractExtension(file);
            String filename = UUID.randomUUID() + extension;
            Path targetPath = directory.resolve(filename).normalize();

            // Store new file
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Delete old file if present
            String oldImageUrl = customer.getProfileImageUrl();
            if (oldImageUrl != null) {
                deletePhysicalFile(oldImageUrl);
            }

            String newImageUrl = URL_PREFIX + filename;
            customer.setProfileImageUrl(newImageUrl);
            customer.setModifiedAt(LocalDateTime.now());
            customerRepository.save(customer);

            return CustomerProfileImageResponse.builder()
                    .profileImageUrl(newImageUrl)
                    .message(successMessage)
                    .build();

        } catch (IOException e) {
            log.error("Failed to store profile image for customer {}: {}", customerId, e.getMessage(), e);
            throw new RuntimeException("Could not store profile image: " + e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Image file is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidRequestException("Image file must not exceed 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !MIME_TO_EXT.containsKey(contentType.toLowerCase())) {
            throw new InvalidRequestException("Only JPG, JPEG, PNG, GIF, and WEBP images are supported");
        }
    }

    private String extractExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && MIME_TO_EXT.containsKey(contentType.toLowerCase())) {
            return MIME_TO_EXT.get(contentType.toLowerCase());
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp)")) {
                return ext;
            }
        }
        return ".jpg";
    }

    private Path getStorageDirectory() {
        return Paths.get(uploadDir, PROFILE_IMAGE_SUBDIR).toAbsolutePath().normalize();
    }

    private void deletePhysicalFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(URL_PREFIX)) {
            return;
        }

        String filename = fileUrl.substring(URL_PREFIX.length());
        Path directory = getStorageDirectory();
        Path target = directory.resolve(filename).normalize();

        // Security check: ensure path does not escape the target directory
        if (!target.getParent().equals(directory)) {
            log.warn("Attempted directory traversal detected for file: {}", filename);
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Could not delete old profile image file {}: {}", target, e.getMessage());
        }
    }
}
