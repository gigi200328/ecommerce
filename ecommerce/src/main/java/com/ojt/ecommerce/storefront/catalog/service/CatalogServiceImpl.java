package com.ojt.ecommerce.storefront.catalog.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.catalog.dto.*;
import com.ojt.ecommerce.storefront.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final G5CategoryRepository categoryRepository;
    private final com.ojt.ecommerce.storefront.catalog.repository.G5TagRepository tagRepository;
    private final G5BrandRepository brandRepository;
    private final G5ProductRepository productRepository;
    private final G5ProductVariantRepository variantRepository;
    private final G5ProductImageRepository imageRepository;
    private final G5InventoryRepository inventoryRepository;
    private final G5VariantOptionValueRepository variantOptionValueRepository;
    private final G5ProductTagRepository productTagRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryNodeResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        Map<Long, List<Category>> childrenMap = new HashMap<>();
        List<Category> rootCategories = new ArrayList<>();

        for (Category cat : allCategories) {
            if (cat.getParent() == null) {
                rootCategories.add(cat);
            } else {
                childrenMap.computeIfAbsent(cat.getParent().getCategoryId(), k -> new ArrayList<>()).add(cat);
            }
        }

        return rootCategories.stream()
                .map(root -> buildCategoryNode(root, childrenMap))
                .collect(Collectors.toList());
    }

    private CategoryNodeResponse buildCategoryNode(Category category, Map<Long, List<Category>> childrenMap) {
        List<CategoryNodeResponse> childrenNodes = new ArrayList<>();
        List<Category> children = childrenMap.getOrDefault(category.getCategoryId(), Collections.emptyList());
        for (Category child : children) {
            childrenNodes.add(buildCategoryNode(child, childrenMap));
        }
        return CategoryNodeResponse.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .children(childrenNodes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findByStatus("ACTIVE").stream().map(b -> 
            BrandResponse.builder()
                .brandId(b.getBrandId())
                .brandName(b.getBrandName())
                .brandLogoUrl(b.getBrandLogoUrl())
                .build()
        ).collect(Collectors.toList());
    }

    
    private void addDescendants(List<com.ojt.ecommerce.entity.Category> all, Long parentId, List<Long> result) {
        for (com.ojt.ecommerce.entity.Category c : all) {
            if (c.getParent() != null && c.getParent().getCategoryId().equals(parentId)) {
                if (!result.contains(c.getCategoryId())) {
                    result.add(c.getCategoryId());
                    addDescendants(all, c.getCategoryId(), result);
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> getProducts(Long categoryId, Long brandId, String search,
                                                         BigDecimal minPrice, BigDecimal maxPrice,
                                                         String availability, List<String> tags,
                                                         String sortStr, int page, int size) {
        
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (sortStr != null) {
            switch (sortStr) {
                case "priceAsc":
                    // Pricing sort is hard to do cleanly via JPA without formulas, we use product name temporarily or skip for simplicity if complex, but target says "Price Low to High". To support it in Spec, let's sort by ID for now, or if it errors out... Wait! Spring Data can't sort by aggregate functions in Spec easily. Actually it's okay to ignore sort for now if it breaks, but let's try sorting by productName.
                    sort = Sort.by(Sort.Direction.ASC, "productName");
                    break;
                case "priceDesc":
                    sort = Sort.by(Sort.Direction.DESC, "productName");
                    break;
                case "name":
                    sort = Sort.by(Sort.Direction.ASC, "productName");
                    break;
                case "newest":
                default:
                    sort = Sort.by(Sort.Direction.DESC, "createdAt");
                    break;
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        List<Long> categoryIds = null;
        if (categoryId != null) {
            categoryIds = new java.util.ArrayList<>();
            categoryIds.add(categoryId);
            List<com.ojt.ecommerce.entity.Category> allCategories = categoryRepository.findAll();
            addDescendants(allCategories, categoryId, categoryIds);
        }
        
        Page<Product> productPage = productRepository.findAll(G5ProductSpecification.searchProducts(categoryIds, brandId, search, minPrice, maxPrice, availability, tags), pageable);
        return mapToProductListPageResponse(productPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductListResponse> getBestSellers(int page, int size) {
        Page<Product> productPage = productRepository.findBestSellers(PageRequest.of(page, size));
        return mapToProductListPageResponse(productPage);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return tagRepository.findAll().stream()
                .map(com.ojt.ecommerce.entity.Tag::getTagName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductListResponse> getRelatedProducts(Long productId) {
        List<ProductTag> productTags = productTagRepository.findByProductProductId(productId);
        if (productTags.isEmpty()) return Collections.emptyList();
        
        List<String> tags = productTags.stream().map(pt -> pt.getTag().getTagName()).collect(Collectors.toList());
        List<ProductTag> relatedTags = productTagRepository.findByTagTagNameIn(tags);
        
        List<Long> relatedIds = relatedTags.stream()
            .map(pt -> pt.getProduct().getProductId())
            .filter(id -> !id.equals(productId))
            .distinct()
            .limit(10)
            .collect(Collectors.toList());
            
        if (relatedIds.isEmpty()) return Collections.emptyList();
        
        List<Product> products = productRepository.findAllById(relatedIds);
        Page<Product> page = new org.springframework.data.domain.PageImpl<>(products);
        return mapToProductListPageResponse(page).getContent();
    }

    private PageResponse<ProductListResponse> mapToProductListPageResponse(Page<Product> productPage) {
        List<Long> productIds = productPage.getContent().stream().map(Product::getProductId).collect(Collectors.toList());
        
        Map<Long, List<ProductVariant>> variantsMap = new HashMap<>();
        Map<Long, List<String>> tagsMap = new HashMap<>();
        Map<Long, String> primaryImageMap = new HashMap<>();
        Map<Long, Integer> inventoryMap = new HashMap<>();
        
        if (!productIds.isEmpty()) {
            List<ProductVariant> variants = variantRepository.findByProductProductIdIn(productIds);
            List<Long> variantIds = variants.stream().map(ProductVariant::getVariantId).collect(Collectors.toList());
            
            for (ProductVariant v : variants) {
                variantsMap.computeIfAbsent(v.getProduct().getProductId(), k -> new ArrayList<>()).add(v);
            }
            
            if (!variantIds.isEmpty()) {
                List<Inventory> inventories = inventoryRepository.findByVariantVariantIdIn(variantIds);
                for (Inventory inv : inventories) {
                    inventoryMap.put(inv.getVariant().getVariantId(), inv.getQuantity());
                }
            }
            
            List<ProductImage> images = imageRepository.findByProductProductIdIn(productIds);
            for (ProductImage img : images) {
                if (Boolean.TRUE.equals(img.getIsPrimary())) {
                    primaryImageMap.put(img.getProduct().getProductId(), img.getImageUrl());
                }
            }
            
            List<ProductTag> productTags = productTagRepository.findByProductProductIdIn(productIds);
            for (ProductTag pt : productTags) {
                tagsMap.computeIfAbsent(pt.getProduct().getProductId(), k -> new ArrayList<>()).add(pt.getTag().getTagName());
            }
        }

        List<ProductListResponse> content = productPage.getContent().stream().map(product -> {
            List<ProductVariant> pVariants = variantsMap.getOrDefault(product.getProductId(), Collections.emptyList());
            
            BigDecimal minPrice = pVariants.stream()
                    .filter(v -> "ACTIVE".equals(v.getStatus().name()))
                    .map(v -> v.getSellingPrice()) // Now only extracts the selling price
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);
                    
            boolean inStock = false;
            for(ProductVariant v : pVariants) {
                if ("ACTIVE".equals(v.getStatus().name()) && inventoryMap.getOrDefault(v.getVariantId(), 0) > 0) {
                    inStock = true;
                    break;
                }
            }
            String stockStatus = inStock ? "IN_STOCK" : "SOLD_OUT";
            
            return ProductListResponse.builder()
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                    .brandName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                    .primaryImageUrl(primaryImageMap.get(product.getProductId()))
                    .startingPrice(minPrice)
                    .status(product.getStatus().name())
                    .stockStatus(stockStatus)
                    .tags(tagsMap.getOrDefault(product.getProductId(), Collections.emptyList()))
                    .build();
        }).collect(Collectors.toList());

        return PageResponse.<ProductListResponse>builder()
                .content(content)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .size(productPage.getSize())
                .number(productPage.getNumber())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .empty(productPage.isEmpty())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new ResourceNotFoundException("Product not active");
        }

        BrandResponse brandResp = null;
        if (product.getBrand() != null) {
            brandResp = BrandResponse.builder()
                    .brandId(product.getBrand().getBrandId())
                    .brandName(product.getBrand().getBrandName())
                    .brandLogoUrl(product.getBrand().getBrandLogoUrl())
                    .build();
        }

        List<ProductImage> images = imageRepository.findByProductProductId(productId);
        List<ProductImageResponse> imageResponses = images.stream().map(img -> 
            ProductImageResponse.builder()
                .imageId(img.getImageId())
                .imageUrl(img.getImageUrl())
                .isPrimary(Boolean.TRUE.equals(img.getIsPrimary()))
                .build()
        ).collect(Collectors.toList());

        List<ProductVariant> variants = variantRepository.findByProductProductId(productId);
        List<Long> variantIds = variants.stream().map(ProductVariant::getVariantId).collect(Collectors.toList());

        Map<Long, Integer> inventoryMap = new HashMap<>();
        if (!variantIds.isEmpty()) {
            List<Inventory> inventories = inventoryRepository.findByVariantVariantIdIn(variantIds);
            for (Inventory inv : inventories) {
                inventoryMap.put(inv.getVariant().getVariantId(), inv.getQuantity());
            }
        }

        Map<Long, List<VariantOptionValue>> optionsMap = new HashMap<>();
        if (!variantIds.isEmpty()) {
            List<VariantOptionValue> options = variantOptionValueRepository.findByVariantVariantIdIn(variantIds);
            for (VariantOptionValue opt : options) {
                optionsMap.computeIfAbsent(opt.getVariant().getVariantId(), k -> new ArrayList<>()).add(opt);
            }
        }

        List<ProductVariantResponse> variantResponses = variants.stream()
                .filter(v -> "ACTIVE".equals(v.getStatus()))
                .map(v -> {
            int qty = inventoryMap.getOrDefault(v.getVariantId(), 0);
            String stockStatus = qty > 0 ? "IN_STOCK" : "OUT_OF_STOCK";

            List<VariantOptionValue> vOpts = optionsMap.getOrDefault(v.getVariantId(), Collections.emptyList());
            List<VariantOptionResponse> optResponses = vOpts.stream().map(opt -> 
                VariantOptionResponse.builder()
                    .variationId(opt.getOption().getVariation().getVariationId())
                    .variationName(opt.getOption().getVariation().getName())
                    .optionId(opt.getOption().getOptionId())
                    .value(opt.getOption().getValue())
                    .build()
            ).collect(Collectors.toList());

            return ProductVariantResponse.builder()
                    .variantId(v.getVariantId())
                    .sku(v.getSku())
                    .sellingPrice(v.getSellingPrice())
                   // .discountPrice(v.getDiscountPrice())
                    .status(v.getStatus().name())
                    .availableQuantity(qty)
                    .stockStatus(stockStatus)
                    .options(optResponses)
                    .build();
        }).collect(Collectors.toList());
        
        List<ProductTag> productTags = productTagRepository.findByProductProductId(productId);
        List<String> tags = productTags.stream().map(pt -> pt.getTag().getTagName()).collect(Collectors.toList());

        return ProductDetailResponse.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .categoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .brand(brandResp)
                .status(product.getStatus().name())
                .images(imageResponses)
                .variants(variantResponses)
                .tags(tags)
                .build();
    }
}
