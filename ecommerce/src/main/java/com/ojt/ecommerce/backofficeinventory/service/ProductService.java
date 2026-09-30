package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.ProductRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.ProductResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.ProductMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductImageRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductImage;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.ProductStatus;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
    private final ProductMapper productMapper;
    private final ProductImageRepository productImageRepository;

    // 1. CREATE
    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + request.getBrandId()));
        }

    
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        LocalDateTime now = LocalDateTime.now();

        Product product = Product.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .status(request.getStatus())
                .category(category)
                .brand(brand)
                .createdBy(user)
                .createdAt(now)
                .modifiedBy(user)
                .modifiedAt(now)
                .build();

        return productMapper.toResponseDto(productRepository.save(product));
    }

    private Page<ProductResponseDto> mapPageWithImages(Page<Product> productPage) {
        List<Long> productIds = productPage.getContent().stream().map(Product::getProductId).toList();
        Map<Long, String> imageMap = new HashMap<>();
        if (!productIds.isEmpty()) {
            List<ProductImage> images = productImageRepository.findByProductProductIdIn(productIds);
            for (ProductImage img : images) {
                Long pId = img.getProduct().getProductId();
                if (!imageMap.containsKey(pId) || Boolean.TRUE.equals(img.getIsPrimary())) {
                    imageMap.put(pId, img.getImageUrl());
                }
            }
        }
        return productPage.map(p -> productMapper.toResponseDto(p, imageMap.get(p.getProductId())));
    }

    // 2. READ ALL (PAGINATED)
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProducts(Pageable pageable) {
        return mapPageWithImages(productRepository.findAll(pageable));
    }

    // 3. READ BY ID
    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        String imageUrl = productImageRepository.findFirstByProductProductIdOrderByIsPrimaryDescImageIdAsc(id)
                .map(ProductImage::getImageUrl)
                .orElse(null);
        return productMapper.toResponseDto(product, imageUrl);
    }

    // 4. READ BY CATEGORY
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductsByCategoryId(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Category not found: " + categoryId);
        }
        return mapPageWithImages(productRepository.findByCategoryCategoryId(categoryId, pageable));
    }

    // 5. READ BY BRAND
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductsByBrandId(Long brandId, Pageable pageable) {
        if (!brandRepository.existsById(brandId)) {
            throw new EntityNotFoundException("Brand not found: " + brandId);
        }
        return mapPageWithImages(productRepository.findByBrandBrandId(brandId, pageable));
    }

    // 6. READ BY STATUS
    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getProductsByStatus(ProductStatus status, Pageable pageable) {
        return mapPageWithImages(productRepository.findByStatus(status, pageable));
    }

    // 7. UPDATE
    @Transactional
    public ProductResponseDto updateProduct(Long id, ProductRequestDto request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + request.getBrandId()));
        }


        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));

        product.setProductName(request.getProductName());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus());
        product.setCategory(category);
        product.setBrand(brand);
        product.setModifiedBy(user);
        product.setModifiedAt(LocalDateTime.now());

        Product saved = productRepository.save(product);
        String imageUrl = productImageRepository.findFirstByProductProductIdOrderByIsPrimaryDescImageIdAsc(id)
                .map(ProductImage::getImageUrl)
                .orElse(null);
        return productMapper.toResponseDto(saved, imageUrl);
    }

    // 8. DELETE
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }
}