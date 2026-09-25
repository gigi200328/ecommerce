package com.ojt.ecommerce.storefront.catalog.service;
import com.ojt.ecommerce.storefront.catalog.dto.*;
import java.math.BigDecimal;
import java.util.List;
public interface CatalogService {
    List<CategoryNodeResponse> getCategoryTree();
    List<BrandResponse> getActiveBrands();
    PageResponse<ProductListResponse> getProducts(Long categoryId, Long brandId, String search, 
                                                  BigDecimal minPrice, BigDecimal maxPrice, 
                                                  String availability, List<String> tags, 
                                                  String sort, int page, int size);
    PageResponse<ProductListResponse> getBestSellers(int page, int size);
    List<ProductListResponse> getRelatedProducts(Long productId);
    List<String> getAllTags();
    ProductDetailResponse getProductDetail(Long productId);
}
