
package com.ojt.ecommerce.storefront.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.ecommerce.storefront.catalog.dto.*;
import com.ojt.ecommerce.storefront.catalog.service.CatalogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogControllerTest {

    private CatalogService catalogService;
    private MockMvc mockMvc;

    private static final String BASE = "/api/storefront/v1";

    @BeforeEach
    void setUp() {

        catalogService = mock(CatalogService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CatalogController(catalogService)
                )
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(
                                new ObjectMapper()
                        )
                )
                .build();
    }

    // ============================================
    // TEST 1 - GET ALL TAGS
    // ============================================

    @Test
    void tags_returns200AndTags() throws Exception {

        when(catalogService.getAllTags())
                .thenReturn(List.of("Mobile", "Electronics"));

        mockMvc.perform(
                get(BASE + "/tags")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$[0]").value("Mobile"))
                .andExpect(jsonPath("$[1]").value("Electronics"));

        verify(catalogService, times(1)).getAllTags();
    }

    // ============================================
    // TEST 2 - CATEGORY TREE
    // ============================================

    @Test
    void categoryTree_returns200() throws Exception {

        when(catalogService.getCategoryTree())
                .thenReturn(List.of());

        mockMvc.perform(
                get(BASE + "/categories/tree")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(catalogService, times(1)).getCategoryTree();
    }

    // ============================================
    // TEST 3 - ACTIVE BRANDS
    // ============================================

    @Test
    void brands_returns200() throws Exception {

        when(catalogService.getActiveBrands())
                .thenReturn(List.of());

        mockMvc.perform(
                get(BASE + "/brands")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(catalogService, times(1)).getActiveBrands();
    }

    // ============================================
    // TEST 4 - PRODUCTS WITH FILTERS
    // ============================================

    @Test
    void products_passesFiltersAndPagination()
            throws Exception {

        when(catalogService.getProducts(
                eq(2L),
                eq(3L),
                eq("iphone"),
                eq(new BigDecimal("100000")),
                eq(new BigDecimal("5000000")),
                eq("IN_STOCK"),
                eq(List.of("Mobile")),
                eq("newest"),
                eq(1),
                eq(12)
        )).thenReturn(emptyPage(1, 12));

        mockMvc.perform(
                get(BASE + "/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("categoryId", "2")
                        .param("brandId", "3")
                        .param("search", "iphone")
                        .param("minPrice", "100000")
                        .param("maxPrice", "5000000")
                        .param("availability", "IN_STOCK")
                        .param("tags", "Mobile")
                        .param("sort", "newest")
                        .param("page", "1")
                        .param("size", "12")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.content").isArray());

        verify(catalogService, times(1))
                .getProducts(
                        eq(2L),
                        eq(3L),
                        eq("iphone"),
                        eq(new BigDecimal("100000")),
                        eq(new BigDecimal("5000000")),
                        eq("IN_STOCK"),
                        eq(List.of("Mobile")),
                        eq("newest"),
                        eq(1),
                        eq(12)
                );
    }

    // ============================================
    // TEST 5 - DEFAULT PAGINATION
    // ============================================

    @Test
    void products_usesDefaultPageAndSize()
            throws Exception {

        when(catalogService.getProducts(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(0),
                eq(20)
        )).thenReturn(emptyPage(0, 20));

        mockMvc.perform(
                get(BASE + "/products")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.empty").value(true));

        verify(catalogService, times(1))
                .getProducts(
                        null, null, null, null,
                        null, null, null, null,
                        0, 20
                );
    }

    // ============================================
    // TEST 6 - BEST SELLERS
    // ============================================

    @Test
    void bestSellers_usesDefaults()
            throws Exception {

        when(catalogService.getBestSellers(0, 20))
                .thenReturn(emptyPage(0, 20));

        mockMvc.perform(
                get(BASE + "/products/best-sellers")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(catalogService, times(1))
                .getBestSellers(0, 20);
    }

    // ============================================
    // TEST 7 - TRENDING PRODUCTS
    // ============================================

    @Test
    void trending_usesDefaults()
            throws Exception {

        when(catalogService.getTrendingProducts(0, 4))
                .thenReturn(emptyPage(0, 4));

        mockMvc.perform(
                get(BASE + "/products/trending")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(4));

        verify(catalogService, times(1))
                .getTrendingProducts(0, 4);
    }

    // ============================================
    // TEST 8 - PRODUCT SUGGESTIONS
    // ============================================

    @Test
    void suggestions_passesPrefix()
            throws Exception {

        when(catalogService.getTrendingSuggestions("iph"))
                .thenReturn(List.of());

        mockMvc.perform(
                get(BASE + "/products/suggestions")
                        .accept(MediaType.APPLICATION_JSON)
                        .param("prefix", "iph")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(catalogService, times(1))
                .getTrendingSuggestions("iph");
    }

    // ============================================
    // TEST 9 - MISSING PREFIX
    // ============================================

    @Test
    void suggestions_withoutPrefix_returns400()
            throws Exception {

        mockMvc.perform(
                get(BASE + "/products/suggestions")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalogService);
    }

    // ============================================
    // TEST 10 - PRODUCT DETAIL + VARIANT IMAGES
    // ============================================

    @Test
    void productDetail_returnsProductAndVariantImages()
            throws Exception {

        ProductVariantImageResponse image =
                ProductVariantImageResponse.builder()
                        .imageId(101L)
                        .imageUrl(
                                "/uploads/product-images/test.jpg"
                        )
                        .isPrimary(true)
                        .build();

        ProductVariantResponse variant =
                ProductVariantResponse.builder()
                        .variantId(10L)
                        .sku("IPH16PRO-128-BLK")
                        .images(List.of(image))
                        .build();

        ProductDetailResponse detail =
                ProductDetailResponse.builder()
                        .productId(1L)
                        .productName("iPhone 16 Pro")
                        .images(List.of())
                        .variants(List.of(variant))
                        .build();

        when(catalogService.getProductDetail(1L))
                .thenReturn(detail);

        mockMvc.perform(
                get(BASE + "/products/1")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productName")
                        .value("iPhone 16 Pro"))
                .andExpect(jsonPath("$.variants[0].variantId")
                        .value(10))
                .andExpect(jsonPath("$.variants[0].sku")
                        .value("IPH16PRO-128-BLK"))
                .andExpect(jsonPath(
                        "$.variants[0].images[0].imageUrl"
                ).value(
                        "/uploads/product-images/test.jpg"
                ));

        verify(catalogService, times(1))
                .getProductDetail(1L);
    }

    // ============================================
    // TEST 11 - RELATED PRODUCTS
    // ============================================

    @Test
    void relatedProducts_returns200()
            throws Exception {

        when(catalogService.getRelatedProducts(1L))
                .thenReturn(List.of());

        mockMvc.perform(
                get(BASE + "/products/1/related")
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(catalogService, times(1))
                .getRelatedProducts(1L);
    }

    // ============================================
    // HELPER METHOD - PAGE RESPONSE
    // ============================================

    private PageResponse<ProductListResponse> emptyPage(
            int number,
            int size) {

        return PageResponse
                .<ProductListResponse>builder()
                .content(List.of())
                .number(number)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }
}
