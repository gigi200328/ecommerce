package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.ProductVariantStatus;
import com.ojt.ecommerce.entity.VariationOption;

@ExtendWith(MockitoExtension.class)
public class ProductVariantServiceImplTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VariationOptionRepository variationOptionRepository;

    @InjectMocks
    private ProductVariantServiceImpl productVariantService;

    private Product mockProduct;
    private ProductVariant mockVariant;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .productId(1L)
                .productName("iPhone 15 Pro")
                .build();

        mockVariant = ProductVariant.builder()
                .variantId(1L)
                .product(mockProduct)
                .sku("IPHONE-15-PRO-BLK-128")
                .sellingPrice(BigDecimal.valueOf(999.00))
                .costPrice(BigDecimal.valueOf(800.00))
                .status(ProductVariantStatus.ACTIVE)
                .build();
    }

    // =========================================================================
    // 1. GET ALL PRODUCT VARIANTS
    // =========================================================================
    @Test
    @DisplayName("Test 1: getAllProductVariants returns list sorted descending by variantId")
    void testGetAllProductVariants() {
        // Arrange
        when(productVariantRepository.findAll(any(Sort.class))).thenReturn(List.of(mockVariant));

        // Act
        List<ProductVariant> result = productVariantService.getAllProductVariants();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IPHONE-15-PRO-BLK-128", result.get(0).getSku());
        verify(productVariantRepository, times(1)).findAll(any(Sort.class));
    }

    // =========================================================================
    // 2. GET VARIANT BY ID
    // =========================================================================
    @Test
    @DisplayName("Test 2: getProductVariantById returns variant when found")
    void testGetProductVariantById_Success() {
        // Arrange
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));

        // Act
        ProductVariant result = productVariantService.getProductVariantById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getVariantId());
        assertEquals("IPHONE-15-PRO-BLK-128", result.getSku());
        verify(productVariantRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Test 3: getProductVariantById throws exception when not found")
    void testGetProductVariantById_NotFound() {
        // Arrange
        when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productVariantService.getProductVariantById(99L));
        assertTrue(ex.getMessage().contains("Product variant not found"));
    }

    // =========================================================================
    // 3. CREATE PRODUCT VARIANT
    // =========================================================================
    @Test
    @DisplayName("Test 4: createProductVariant saves new variant successfully")
    void testCreateProductVariant_Success() {
        // Arrange
        ProductVariant newVariant = ProductVariant.builder()
                .product(Product.builder().productId(1L).build())
                .sku("IPHONE-15-PRO-TIT-256")
                .sellingPrice(BigDecimal.valueOf(1099.00))
                .status(ProductVariantStatus.ACTIVE)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("IPHONE-15-PRO-TIT-256")).thenReturn(false);
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(newVariant);

        // Act
        ProductVariant result = productVariantService.createProductVariant(newVariant);

        // Assert
        assertNotNull(result);
        assertEquals("IPHONE-15-PRO-TIT-256", result.getSku());
        verify(productVariantRepository, times(1)).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Test 5: createProductVariant throws exception on duplicate SKU")
    void testCreateProductVariant_DuplicateSku() {
        // Arrange
        ProductVariant duplicateVariant = ProductVariant.builder()
                .product(Product.builder().productId(1L).build())
                .sku("IPHONE-15-PRO-BLK-128")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productVariantRepository.existsBySku("IPHONE-15-PRO-BLK-128")).thenReturn(true);

        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productVariantService.createProductVariant(duplicateVariant));
        assertTrue(ex.getMessage().contains("already exists"));
        verify(productVariantRepository, never()).save(any());
    }

    // =========================================================================
    // 4. GET VARIANTS BY PRODUCT ID
    // =========================================================================
    @Test
    @DisplayName("Test 6: getVariantsByProductId returns all variants for given product")
    void testGetVariantsByProductId_Success() {
        // Arrange
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productVariantRepository.findByProduct_ProductId(1L)).thenReturn(List.of(mockVariant));

        // Act
        List<ProductVariant> result = productVariantService.getVariantsByProductId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(productVariantRepository, times(1)).findByProduct_ProductId(1L);
    }

    @Test
    @DisplayName("Test 7: getVariantsByProductId throws exception if product does not exist")
    void testGetVariantsByProductId_ProductNotFound() {
        // Arrange
        when(productRepository.existsById(99L)).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> productVariantService.getVariantsByProductId(99L));
    }

    // =========================================================================
    // 5. UPDATE PRODUCT VARIANT
    // =========================================================================
    @Test
    @DisplayName("Test 8: updateProductVariant updates price and status")
    void testUpdateProductVariant_Success() {
        // Arrange
        ProductVariant updatePayload = ProductVariant.builder()
                .sellingPrice(BigDecimal.valueOf(949.00))
                .status(ProductVariantStatus.INACTIVE)
                .build();

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(productVariantRepository.save(any(ProductVariant.class))).thenReturn(mockVariant);

        // Act
        ProductVariant result = productVariantService.updateProductVariant(1L, updatePayload);

        // Assert
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(949.00), mockVariant.getSellingPrice());
        assertEquals(ProductVariantStatus.INACTIVE, mockVariant.getStatus());
        verify(productVariantRepository, times(1)).save(mockVariant);
    }

    // =========================================================================
    // 6. DELETE PRODUCT VARIANT
    // =========================================================================
    @Test
    @DisplayName("Test 9: deleteProductVariant removes variant from repository")
    void testDeleteProductVariant_Success() {
        // Arrange
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));

        // Act
        productVariantService.deleteProductVariant(1L);

        // Assert
        verify(productVariantRepository, times(1)).delete(mockVariant);
    }

    // =========================================================================
    // 7. GENERATE SKU
    // =========================================================================
    @Test
    @DisplayName("Test 10: generateSku creates patterned SKU from product and option values")
    void testGenerateSku() {
        // Arrange
        VariationOption opt1 = VariationOption.builder().optionId(101L).value("Black").build();
        VariationOption opt2 = VariationOption.builder().optionId(102L).value("128GB").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(variationOptionRepository.findAllById(List.of(101L, 102L))).thenReturn(List.of(opt1, opt2));

        // Act
        String generatedSku = productVariantService.generateSku(1L, List.of(101L, 102L));

        // Assert
        assertNotNull(generatedSku);
        assertTrue(generatedSku.startsWith("IPHONE-15-PRO"));
        assertTrue(generatedSku.contains("BLACK"));
        assertTrue(generatedSku.contains("128GB"));
    }
}
