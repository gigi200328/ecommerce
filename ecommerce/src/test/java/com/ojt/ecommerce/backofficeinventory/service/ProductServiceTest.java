package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ojt.ecommerce.backofficeinventory.dto.ProductRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.ProductMapper;
import com.ojt.ecommerce.backofficeinventory.repository.*;
import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.enums.ProductStatus;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Brand brand;
    private User user;
    private Product product;
    private ProductResponseDto responseDto;

    @BeforeEach
    void setUp() {
        category = Category.builder().categoryName("Electronics").build();
        category.setCategoryId(1L);

        brand = Brand.builder().brandName("Apple").build();
        brand.setBrandId(1L);

        user = mock(User.class);
        responseDto = mock(ProductResponseDto.class);

        product = Product.builder()
                .productName("iPhone")
                .description("Smartphone")
                .status(ProductStatus.ACTIVE)
                .category(category)
                .brand(brand)
                .build();
        product.setProductId(1L);
    }

    private ProductRequestDto request(Long categoryId, Long brandId, Long userId) {
        ProductRequestDto req = new ProductRequestDto();
        req.setProductName("iPhone");
        req.setDescription("Smartphone");
        req.setStatus(ProductStatus.ACTIVE);
        req.setCategoryId(categoryId);
        req.setBrandId(brandId);
        req.setUserId(userId);
        return req;
    }

    // =========================================================
    // CREATE PRODUCT TESTS
    // =========================================================

    @Test
    void createProduct_success() {
        ProductRequestDto req = request(1L, 1L, 1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponseDto(product)).thenReturn(responseDto);

        ProductResponseDto result = productService.createProduct(req);

        assertSame(responseDto, result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_whenCategoryNotFound_throwsException() {
        ProductRequestDto req = request(99L, 1L, 1L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.createProduct(req));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenBrandNotFound_throwsException() {
        ProductRequestDto req = request(1L, 99L, 1L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.createProduct(req));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_whenUserNotFound_throwsException() {
        ProductRequestDto req = request(1L, 1L, 99L);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.createProduct(req));
        verify(productRepository, never()).save(any());
    }

    // =========================================================
    // READ ALL & MAPPING TESTS
    // =========================================================

    @Test
    void getAllProducts_returnsPagedResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(pageable)).thenReturn(page);
        when(productImageRepository.findByProductProductIdIn(anyList())).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductIdIn(anyList())).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        Page<ProductResponseDto> result = productService.getAllProducts(pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));
    }

    // =========================================================
    // READ BY ID TESTS
    // =========================================================

    @Test
    void getProductById_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productImageRepository.findFirstByProductProductIdOrderByIsPrimaryDescImageIdAsc(1L))
                .thenReturn(Optional.empty());
        when(productVariantRepository.findByProduct_ProductId(1L)).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        ProductResponseDto result = productService.getProductById(1L);

        assertSame(responseDto, result);
    }

    @Test
    void getProductById_whenNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.getProductById(99L));
    }

    // =========================================================
    // READ BY CATEGORY / BRAND / STATUS TESTS
    // =========================================================

    @Test
    void getProductsByCategoryId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByCategoryCategoryId(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productImageRepository.findByProductProductIdIn(anyList())).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductIdIn(anyList())).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        Page<ProductResponseDto> result = productService.getProductsByCategoryId(1L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProductsByCategoryId_whenMissing_throwsException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> productService.getProductsByCategoryId(99L, pageable));
    }

    @Test
    void getProductsByBrandId_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(brandRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findByBrandBrandId(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productImageRepository.findByProductProductIdIn(anyList())).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductIdIn(anyList())).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        Page<ProductResponseDto> result = productService.getProductsByBrandId(1L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProductsByBrandId_whenMissing_throwsException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(brandRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> productService.getProductsByBrandId(99L, pageable));
    }

    @Test
    void getProductsByStatus_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByStatus(eq(ProductStatus.ACTIVE), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productImageRepository.findByProductProductIdIn(anyList())).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductIdIn(anyList())).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        Page<ProductResponseDto> result = productService.getProductsByStatus(ProductStatus.ACTIVE, pageable);

        assertEquals(1, result.getTotalElements());
    }

    // =========================================================
    // UPDATE PRODUCT TESTS
    // =========================================================

    @Test
    void updateProduct_success() {
        ProductRequestDto req = request(1L, 1L, 1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.save(product)).thenReturn(product);
        when(productImageRepository.findFirstByProductProductIdOrderByIsPrimaryDescImageIdAsc(1L))
                .thenReturn(Optional.empty());
        when(productVariantRepository.findByProduct_ProductId(1L)).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        ProductResponseDto result = productService.updateProduct(1L, req);

        assertSame(responseDto, result);
        verify(productRepository).save(product);
    }

    @Test
    void updateProduct_whenNotFound_throwsException() {
        ProductRequestDto req = request(1L, 1L, 1L);
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> productService.updateProduct(99L, req));
    }

    // =========================================================
    // DELETE PRODUCT TESTS
    // =========================================================

    @Test
    void deleteProduct_success() {
        when(productRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));
        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteProduct_whenNotFound_throwsException() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> productService.deleteProduct(99L));
        verify(productRepository, never()).deleteById(any());
    }

    // =========================================================
    // SEARCH PRODUCTS TESTS
    // =========================================================

    @Test
    void searchProducts_success() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productImageRepository.findByProductProductIdIn(anyList())).thenReturn(List.of());
        when(productVariantRepository.findByProduct_ProductIdIn(anyList())).thenReturn(List.of());
        when(productMapper.toResponseDto(eq(product), any(), any(), any(), any())).thenReturn(responseDto);

        Page<ProductResponseDto> result = productService.searchProducts(
                "iPhone", 1L, 1L, ProductStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.TEN, pageable
        );

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));
    }
}