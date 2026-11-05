package com.ojt.ecommerce.storefront.catalog.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.enums.BrandStatus;
import com.ojt.ecommerce.storefront.catalog.dto.*;
import com.ojt.ecommerce.storefront.catalog.repository.*;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.searchhistory.service.SearchHistoryService;
import com.ojt.ecommerce.storefront.searchhistory.repository.SearchKeywordStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.PageRequest;
import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ojt.ecommerce.enums.ProductStatus;
import com.ojt.ecommerce.entity.ProductVariantStatus;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {
    @Mock private G5CategoryRepository categoryRepository;
    @Mock private G5TagRepository tagRepository;
    @Mock private G5BrandRepository brandRepository;
    @Mock private G5ProductRepository productRepository;
    @Mock private G5ProductVariantRepository variantRepository;
    @Mock private G5ProductImageRepository imageRepository;
    @Mock private G5ProductVariantImageRepository variantImageRepository;
    @Mock private G5InventoryRepository inventoryRepository;
    @Mock private G5VariantOptionValueRepository variantOptionValueRepository;
    @Mock private G5ProductTagRepository productTagRepository;
    @Mock private SearchHistoryService searchHistoryService;
    @Mock private SearchKeywordStatsRepository searchKeywordStatsRepository;
    @InjectMocks private CatalogServiceImpl catalogService;

    @Test
    void allTags_mapsTagNames() {
        Tag tag1 = mock(Tag.class);
        Tag tag2 = mock(Tag.class);
        when(tag1.getTagName()).thenReturn("Mobile");
        when(tag2.getTagName()).thenReturn("Electronics");
        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));
        assertEquals(List.of("Mobile", "Electronics"), catalogService.getAllTags());
        verify(tagRepository).findAll();
    }

    @Test
    void categoryTree_buildsNestedChildren() {
        Category root = mock(Category.class);
        Category child = mock(Category.class);
        when(root.getCategoryId()).thenReturn(1L);
        when(root.getCategoryName()).thenReturn("Electronics");
        when(child.getCategoryId()).thenReturn(2L);
        when(child.getCategoryName()).thenReturn("Phones");
        when(child.getParent()).thenReturn(root);
        when(categoryRepository.findAll()).thenReturn(List.of(root, child));
        List<CategoryNodeResponse> tree = catalogService.getCategoryTree();
        assertEquals(1, tree.size());
        assertEquals("Electronics", tree.get(0).getCategoryName());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("Phones", tree.get(0).getChildren().get(0).getCategoryName());
    }

    @Test
    void activeBrands_returnsOnlyRepositoryResults() {
        Brand brand = mock(Brand.class);
        when(brand.getBrandId()).thenReturn(5L);
        when(brand.getBrandName()).thenReturn("Apple");
        when(brandRepository.findByStatus(BrandStatus.ACTIVE)).thenReturn(List.of(brand));
        List<BrandResponse> result = catalogService.getActiveBrands();
        assertEquals(1, result.size());
        assertEquals("Apple", result.get(0).getBrandName());
        verify(brandRepository).findByStatus(BrandStatus.ACTIVE);
    }

    @Test
    void productDetail_missingProduct_throwsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> catalogService.getProductDetail(999L));
        verify(productRepository).findById(999L);
        verifyNoInteractions(variantImageRepository);
    }

    @Test
    void relatedProducts_withoutTags_returnsEmpty() {
        when(productTagRepository.findByProductProductId(1L)).thenReturn(List.of());
        assertTrue(catalogService.getRelatedProducts(1L).isEmpty());
        verifyNoInteractions(productRepository);
    }

    @Test
    void suggestions_blankPrefix_returnsEmptyWithoutQuery() {
        assertTrue(catalogService.getTrendingSuggestions("  ").isEmpty());
        assertTrue(catalogService.getTrendingSuggestions(null).isEmpty());
        verifyNoInteractions(productRepository);
    }

    @Test
    void suggestions_noCandidates_returnsEmpty() {
        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<Product>(List.of()));
        assertTrue(catalogService.getTrendingSuggestions("iphone").isEmpty());
        verifyNoInteractions(searchKeywordStatsRepository);
    }


@Test
void products_emptyPage_returnsEmptyResponse() {

    // Arrange: Empty product page with page size 20
    Page<Product> emptyPage = new PageImpl<>(
            List.of(),
            PageRequest.of(0, 20),
            0
    );

    when(productRepository.findAll(
    		ArgumentMatchers.<Specification<Product>>any(),
            any(Pageable.class)
    )).thenReturn(emptyPage);

    // Act
    PageResponse<ProductListResponse> result =
            catalogService.getProducts(
                    null, null, null, null, null,
                    null, null, null, 0, 20
            );

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());
    assertEquals(0, result.getTotalPages());
    assertEquals(0, result.getNumber());
    assertEquals(20, result.getSize());
    assertTrue(result.isEmpty());

    verify(productRepository, times(1))
            .findAll(
            		ArgumentMatchers.<Specification<Product>>any(),
                    any(Pageable.class)
            );
}


    @Test
    void bestSellers_emptyPage_returnsEmptyResponse() {
        when(productRepository.findBestSellers(any(Pageable.class)))
                .thenReturn(new PageImpl<Product>(List.of()));
        PageResponse<ProductListResponse> result = catalogService.getBestSellers(0, 20);
        assertTrue(result.getContent().isEmpty());
        verify(productRepository).findBestSellers(any(Pageable.class));
    }

    @Test
    void trending_noPopularSearches_returnsEmptyWithoutDatabaseQuery() {
        when(searchHistoryService.getPopularSearches()).thenReturn(List.of());
        PageResponse<ProductListResponse> result = catalogService.getTrendingProducts(0, 4);
        assertTrue(result.getContent().isEmpty());
        assertEquals(4, result.getSize());
        verifyNoInteractions(productRepository);
    }
    

@Test
void products_filtersByCategoryAndPrice() {

    // Arrange
    BigDecimal minPrice = new BigDecimal("50000");
    BigDecimal maxPrice = new BigDecimal("500000");

    when(categoryRepository.findAll())
            .thenReturn(List.of());

    when(productRepository.findAll(
    		ArgumentMatchers.<Specification<Product>>any(),
            any(Pageable.class)
    )).thenReturn(new PageImpl<Product>(List.of()));

    // Act
    PageResponse<ProductListResponse> result =
            catalogService.getProducts(
                    1L,        // categoryId
                    null,      // brandId
                    null,      // search
                    minPrice,
                    maxPrice,
                    null,      // availability
                    null,      // tags
                    null,      // sort
                    0,         // page
                    20         // size
            );

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());

    verify(categoryRepository).findAll();

    verify(productRepository).findAll(
    		ArgumentMatchers.<Specification<Product>>any(),
            any(Pageable.class)
    );
}


@Test
void productDetail_returnsProductWithVariantsAndImages() {

    // Arrange
    Product product = mock(Product.class);
    ProductVariant variant = mock(ProductVariant.class);
    ProductImage image = mock(ProductImage.class);

    // Use actual enum types without hardcoding their names
    when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);

    when(variant.getStatus()).thenReturn(ProductVariantStatus.ACTIVE);

    when(productRepository.findById(1L))
            .thenReturn(Optional.of(product));

    when(product.getProductId()).thenReturn(1L);
    when(product.getProductName()).thenReturn("iPhone 16 Pro");

    when(variantRepository.findByProductProductId(1L))
            .thenReturn(List.of(variant));

    when(variant.getVariantId()).thenReturn(10L);
    when(variant.getSku()).thenReturn("IPH16PRO-128-BLK");

    when(imageRepository.findByProductProductId(1L))
            .thenReturn(List.of(image));

    when(image.getImageUrl())
            .thenReturn("/uploads/product-images/iphone.jpg");

    when(variantImageRepository
            .findByProductVariantVariantIdIn(List.of(10L)))
            .thenReturn(List.of());

    when(inventoryRepository.findByVariantVariantIdIn(List.of(10L)))
            .thenReturn(List.of());

    when(variantOptionValueRepository
            .findByVariantVariantIdIn(List.of(10L)))
            .thenReturn(List.of());

    when(productTagRepository.findByProductProductId(1L))
            .thenReturn(List.of());

    // Act
    ProductDetailResponse result =
            catalogService.getProductDetail(1L);

    // Assert
    assertNotNull(result);
    assertEquals("iPhone 16 Pro", result.getProductName());
    assertEquals(1, result.getImages().size());
    assertEquals(1, result.getVariants().size());
    assertEquals(
            "IPH16PRO-128-BLK",
            result.getVariants().get(0).getSku()
    );
}


@Test
void bestSellers_usesCorrectPagination() {

    // Arrange
    when(productRepository.findBestSellers(
            PageRequest.of(0, 4)
    )).thenReturn(
            new PageImpl<>(
                    List.of(),
                    PageRequest.of(0, 4),
                    0
            )
    );

    // Act
    PageResponse<ProductListResponse> result =
            catalogService.getBestSellers(0, 4);

    // Assert
    assertEquals(4, result.getSize());
    assertTrue(result.getContent().isEmpty());

    verify(productRepository).findBestSellers(
            PageRequest.of(0, 4)
    );
}

@Test
void products_reservedStock_returnsSoldOut() {

    // Arrange
    Product product = mock(Product.class);
    ProductVariant variant = mock(ProductVariant.class);
    Inventory inventory = mock(Inventory.class);

    when(product.getProductId()).thenReturn(1L);

    when(product.getStatus()).thenReturn(ProductStatus.ACTIVE);
    when(variant.getStatus()).thenReturn(ProductVariantStatus.ACTIVE);

    when(variant.getProduct()).thenReturn(product);
    when(variant.getVariantId()).thenReturn(10L);

    when(productRepository.findAll(
    		ArgumentMatchers.<Specification<Product>>any(),
            any(Pageable.class)
    )).thenReturn(new PageImpl<>(List.of(product)));

    when(variantRepository.findByProductProductIdIn(List.of(1L)))
            .thenReturn(List.of(variant));

    when(inventoryRepository.findByVariantVariantIdIn(List.of(10L)))
            .thenReturn(List.of(inventory));

    when(inventory.getVariant()).thenReturn(variant);
    when(inventory.getQuantity()).thenReturn(10);
    when(inventory.getReservedQuantity()).thenReturn(10);

    // Act
    PageResponse<ProductListResponse> result =
            catalogService.getProducts(
                    null, null, null, null, null,
                    null, null, null, 0, 20
            );

    // Assert
    assertEquals(1, result.getContent().size());

    assertEquals(
            "SOLD_OUT",
            result.getContent().get(0).getStockStatus()
    );
}

}
