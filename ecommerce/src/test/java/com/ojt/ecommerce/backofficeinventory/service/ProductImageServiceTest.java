package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.ProductImageRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductImageResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.ProductImageMapper;
import com.ojt.ecommerce.backofficeinventory.repository.ProductImageRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductImage;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.UploadType;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductImageMapper productImageMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductImageService productImageService;

    private User user;
    private Product product;
    private ProductImageResponseDto responseDto;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        product = mock(Product.class);
        responseDto = mock(ProductImageResponseDto.class);

        lenient().when(product.getProductId()).thenReturn(100L);

        // Initialize transaction synchronization for callback registration.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private ProductImageRequestDto request(
            Long productId,
            Long userId,
            MultipartFile file,
            Boolean isPrimary) {

        ProductImageRequestDto request = new ProductImageRequestDto();
        request.setProductId(productId);
        request.setUserId(userId);
        request.setFile(file);
        request.setIsPrimary(isPrimary);

        return request;
    }

    private ProductImage productImage(
            Long imageId,
            Product prod,
            String url,
            boolean isPrimary) {

        ProductImage image = ProductImage.builder()
                .product(prod)
                .imageUrl(url)
                .isPrimary(isPrimary)
                .build();

        image.setImageId(imageId);
        return image;
    }

    // =========================================
    // UPLOAD PRODUCT IMAGE TESTS
    // =========================================

    @Test
    void uploadProductImage_success_nonPrimary() {
        MultipartFile file = mock(MultipartFile.class);
        ProductImageRequestDto request =
                request(100L, 1L, file, false);

        when(productRepository.findById(100L))
                .thenReturn(Optional.of(product));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(fileStorageService.store(file, UploadType.PRODUCT_IMAGE))
                .thenReturn("product-img.png");

        ProductImage savedImage =
                productImage(1L, product, "product-img.png", false);

        when(productImageRepository.save(any(ProductImage.class)))
                .thenReturn(savedImage);
        when(productImageMapper.toResponseDto(savedImage))
                .thenReturn(responseDto);

        ProductImageResponseDto result =
                productImageService.uploadProductImage(request);

        assertSame(responseDto, result);
        verify(fileStorageService).store(file, UploadType.PRODUCT_IMAGE);
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void uploadProductImage_success_primaryUnsetsExisting() {
        MultipartFile file = mock(MultipartFile.class);
        ProductImageRequestDto request =
                request(100L, 1L, file, true);

        when(productRepository.findById(100L))
                .thenReturn(Optional.of(product));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(fileStorageService.store(file, UploadType.PRODUCT_IMAGE))
                .thenReturn("product-img.png");

        ProductImage existingPrimary =
                productImage(2L, product, "old-primary.png", true);

        when(productImageRepository
                .findByProductProductIdAndIsPrimaryTrue(100L))
                .thenReturn(List.of(existingPrimary));

        ProductImage savedImage =
                productImage(1L, product, "product-img.png", true);

        when(productImageRepository.save(any(ProductImage.class)))
                .thenReturn(savedImage);
        when(productImageMapper.toResponseDto(savedImage))
                .thenReturn(responseDto);

        ProductImageResponseDto result =
                productImageService.uploadProductImage(request);

        assertSame(responseDto, result);
        assertFalse(existingPrimary.getIsPrimary());

        verify(productImageRepository).saveAll(anyList());
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void uploadProductImage_whenProductNotFound_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        ProductImageRequestDto request =
                request(999L, 1L, file, false);

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productImageService.uploadProductImage(request));

        verify(productImageRepository, never()).save(any());
    }

    @Test
    void uploadProductImage_whenUserNotFound_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        ProductImageRequestDto request =
                request(100L, 999L, file, false);

        when(productRepository.findById(100L))
                .thenReturn(Optional.of(product));
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productImageService.uploadProductImage(request));

        verify(productImageRepository, never()).save(any());
    }

    // =========================================
    // GET IMAGES BY PRODUCT ID TESTS
    // =========================================

    @Test
    void getImagesByProductId_returnsList() {
        ProductImage image =
                productImage(1L, product, "img.png", true);

        when(productImageRepository.findByProductProductId(100L))
                .thenReturn(List.of(image));
        when(productImageMapper.toResponseDto(image))
                .thenReturn(responseDto);

        List<ProductImageResponseDto> result =
                productImageService.getImagesByProductId(100L);

        assertEquals(1, result.size());
        assertSame(responseDto, result.get(0));
    }

    // =========================================
    // SET PRIMARY IMAGE TESTS
    // =========================================

    @Test
    void setPrimaryImage_success() {
        ProductImage image =
                productImage(1L, product, "img.png", false);

        when(productImageRepository.findById(1L))
                .thenReturn(Optional.of(image));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(productImageRepository
                .findByProductProductIdAndIsPrimaryTrue(100L))
                .thenReturn(List.of());
        when(productImageRepository.save(image))
                .thenReturn(image);
        when(productImageMapper.toResponseDto(image))
                .thenReturn(responseDto);

        ProductImageResponseDto result =
                productImageService.setPrimaryImage(1L, 1L);

        assertSame(responseDto, result);
        assertTrue(image.getIsPrimary());
        verify(productImageRepository).save(image);
    }

    @Test
    void setPrimaryImage_whenImageNotFound_throwsException() {
        when(productImageRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productImageService.setPrimaryImage(999L, 1L));
    }

    @Test
    void setPrimaryImage_whenUserNotFound_throwsException() {
        ProductImage image =
                productImage(1L, product, "img.png", false);

        when(productImageRepository.findById(1L))
                .thenReturn(Optional.of(image));
        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productImageService.setPrimaryImage(1L, 999L));
    }

    // =========================================
    // DELETE PRODUCT IMAGE TESTS
    // =========================================

    @Test
    void deleteProductImage_success() {
        ProductImage image =
                productImage(1L, product, "img.png", false);

        when(productImageRepository.findById(1L))
                .thenReturn(Optional.of(image));

        assertDoesNotThrow(
                () -> productImageService.deleteProductImage(1L));

        verify(productImageRepository).delete(image);

        // Execute the registered afterCommit callback for this unit test.
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(fileStorageService)
                .delete("img.png", UploadType.PRODUCT_IMAGE);
    }

    @Test
    void deleteProductImage_whenNotFound_throwsException() {
        when(productImageRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> productImageService.deleteProductImage(999L));

        verify(productImageRepository, never()).delete(any());
    }
    
    @Test
    void uploadProductImage_whenRollbackCleanupFails_catchesException() {
        MultipartFile file = mock(MultipartFile.class);
        ProductImageRequestDto request = request(100L, 1L, file, false);

        when(productRepository.findById(100L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.store(file, UploadType.PRODUCT_IMAGE)).thenReturn("product-img.png");

        
        when(productImageRepository.save(any(ProductImage.class))).thenThrow(new RuntimeException("DB Error"));

        
        org.mockito.Mockito.doThrow(new RuntimeException("Delete Error"))
                .when(fileStorageService).delete("product-img.png", UploadType.PRODUCT_IMAGE);

        assertThrows(RuntimeException.class, () -> productImageService.uploadProductImage(request));

       
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(fileStorageService).delete("product-img.png", UploadType.PRODUCT_IMAGE);
    }

    @Test
    void deleteProductImage_whenCommitCleanupFails_catchesException() {
        ProductImage image = productImage(1L, product, "img.png", false);
        when(productImageRepository.findById(1L)).thenReturn(Optional.of(image));

      
        org.mockito.Mockito.doThrow(new RuntimeException("Delete Error"))
                .when(fileStorageService).delete("img.png", UploadType.PRODUCT_IMAGE);

        assertDoesNotThrow(() -> productImageService.deleteProductImage(1L));

       
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(fileStorageService).delete("img.png", UploadType.PRODUCT_IMAGE);
    }
}