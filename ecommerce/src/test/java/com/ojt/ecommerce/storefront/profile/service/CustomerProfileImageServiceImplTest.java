package com.ojt.ecommerce.storefront.profile.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.profile.dto.CustomerProfileImageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileImageServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerProfileImageServiceImpl profileImageService;

    @TempDir
    Path tempDir;

    private Customer customer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(profileImageService, "uploadDir", tempDir.toString());

        customer = Customer.builder()
                .customerId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .profileImageUrl(null)
                .build();
    }

    // =========================================================================
    // 1. uploadProfileImage Tests
    // =========================================================================

    @Test
    @DisplayName("Should successfully upload PNG profile image")
    void uploadProfileImage_Png_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake image data".getBytes()
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileImageResponse response = profileImageService.uploadProfileImage(1L, file);

        assertNotNull(response);
        assertEquals("Profile image uploaded successfully", response.getMessage());
        assertNotNull(response.getProfileImageUrl());
        assertTrue(response.getProfileImageUrl().startsWith("/uploads/profile-images/"));
        assertTrue(response.getProfileImageUrl().endsWith(".png"));
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    @DisplayName("Should successfully upload JPEG profile image")
    void uploadProfileImage_Jpeg_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake jpg data".getBytes()
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileImageResponse response = profileImageService.uploadProfileImage(1L, file);

        assertNotNull(response);
        assertTrue(response.getProfileImageUrl().endsWith(".jpg"));
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when file is null")
    void uploadProfileImage_ThrowsException_WhenFileNull() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> profileImageService.uploadProfileImage(1L, null));

        assertEquals("Image file is required", ex.getMessage());
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when file is empty")
    void uploadProfileImage_ThrowsException_WhenFileEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.png", "image/png", new byte[0]
        );

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> profileImageService.uploadProfileImage(1L, emptyFile));

        assertEquals("Image file is required", ex.getMessage());
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when file size exceeds 5MB")
    void uploadProfileImage_ThrowsException_WhenFileTooLarge() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.png", "image/png", largeBytes
        );

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> profileImageService.uploadProfileImage(1L, largeFile));

        assertEquals("Image file must not exceed 5MB", ex.getMessage());
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when MIME type is unsupported")
    void uploadProfileImage_ThrowsException_WhenUnsupportedMimeType() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "pdf content".getBytes()
        );

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> profileImageService.uploadProfileImage(1L, pdfFile));

        assertEquals("Only JPG, JPEG, PNG, GIF, and WEBP images are supported", ex.getMessage());
        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found during upload")
    void uploadProfileImage_ThrowsException_WhenCustomerNotFound() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "valid content".getBytes()
        );

        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> profileImageService.uploadProfileImage(99L, file));

        assertTrue(ex.getMessage().contains("Customer not found with id: 99"));
    }

    // =========================================================================
    // 2. updateProfileImage Tests
    // =========================================================================

    @Test
    @DisplayName("Should successfully update profile image and replace old file")
    void updateProfileImage_Success() throws IOException {
        // Setup old physical file
        Path profileDir = tempDir.resolve("profile-images");
        Files.createDirectories(profileDir);
        Path oldFile = profileDir.resolve("old-photo.jpg");
        Files.writeString(oldFile, "old photo content");
        customer.setProfileImageUrl("/uploads/profile-images/old-photo.jpg");

        MockMultipartFile newFile = new MockMultipartFile(
                "file", "new-avatar.png", "image/png", "new image data".getBytes()
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileImageResponse response = profileImageService.updateProfileImage(1L, newFile);

        assertNotNull(response);
        assertEquals("Profile image updated successfully", response.getMessage());
        assertFalse(Files.exists(oldFile), "Old physical file should be deleted");
    }

    // =========================================================================
    // 3. deleteProfileImage Tests
    // =========================================================================

    @Test
    @DisplayName("Should delete physical profile image file and set profileImageUrl to null")
    void deleteProfileImage_Success() throws IOException {
        Path profileDir = tempDir.resolve("profile-images");
        Files.createDirectories(profileDir);
        Path oldFile = profileDir.resolve("my-photo.jpg");
        Files.writeString(oldFile, "photo data");
        customer.setProfileImageUrl("/uploads/profile-images/my-photo.jpg");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        profileImageService.deleteProfileImage(1L);

        assertNull(customer.getProfileImageUrl());
        assertFalse(Files.exists(oldFile));
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting image of non-existent customer")
    void deleteProfileImage_ThrowsException_WhenCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> profileImageService.deleteProfileImage(99L));

        assertTrue(ex.getMessage().contains("Customer not found with id: 99"));
        verify(customerRepository, never()).save(any());
    }

    // =========================================================================
    // 4. getProfileImageUrl Tests
    // =========================================================================

    @Test
    @DisplayName("Should return customer profile image URL")
    void getProfileImageUrl_Success() {
        customer.setProfileImageUrl("/uploads/profile-images/avatar.png");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        String url = profileImageService.getProfileImageUrl(1L);

        assertEquals("/uploads/profile-images/avatar.png", url);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found during getProfileImageUrl")
    void getProfileImageUrl_ThrowsException_WhenCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> profileImageService.getProfileImageUrl(99L));

        assertTrue(ex.getMessage().contains("Customer not found with id: 99"));
    }

    // =========================================================================
    // 5. updateProfileAvatar Tests
    // =========================================================================

    @Test
    @DisplayName("Should update profile avatar successfully with valid avatar URL")
    void updateProfileAvatar_Success() {
        String avatarUrl = "https://api.dicebear.com/7.x/bottts/svg?seed=user123";
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfileImageResponse response = profileImageService.updateProfileAvatar(1L, avatarUrl);

        assertNotNull(response);
        assertEquals(avatarUrl, response.getProfileImageUrl());
        assertEquals("Profile avatar updated successfully", response.getMessage());
        assertEquals(avatarUrl, customer.getProfileImageUrl());
    }

    @Test
    @DisplayName("Should throw InvalidRequestException when avatar URL is blank or null")
    void updateProfileAvatar_ThrowsException_WhenBlank() {
        InvalidRequestException ex1 = assertThrows(InvalidRequestException.class,
                () -> profileImageService.updateProfileAvatar(1L, "   "));
        assertEquals("Avatar URL is required", ex1.getMessage());

        InvalidRequestException ex2 = assertThrows(InvalidRequestException.class,
                () -> profileImageService.updateProfileAvatar(1L, null));
        assertEquals("Avatar URL is required", ex2.getMessage());

        verifyNoInteractions(customerRepository);
    }
}
