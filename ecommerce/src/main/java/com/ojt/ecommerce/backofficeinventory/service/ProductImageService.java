package com.ojt.ecommerce.backofficeinventory.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductImageMapper productImageMapper;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Transactional
    public ProductImageResponseDto uploadProductImage(
            ProductImageRequestDto request) {

        // 1. Check Product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Product not found with id: "
                                        + request.getProductId()));

        // 2. Check User exists
        // User.userId is Long, so no intValue() is needed.
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found with id: "
                                        + request.getUserId()));

        MultipartFile file = request.getFile();

        // 3. Validate image file
        validateImageFile(file);

        Path targetLocation = null;

        try {

            // 4. Create upload directory if it does not exist
            Path uploadPath = Paths
                    .get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            // 5. Generate unique filename
            String originalFilename = file.getOriginalFilename();

            String fileExtension = "";

            if (originalFilename != null) {

                int lastDot = originalFilename.lastIndexOf(".");

                if (lastDot > 0) {
                    fileExtension = originalFilename
                            .substring(lastDot)
                            .toLowerCase();
                }
            }

            String newFilename =
                    UUID.randomUUID() + fileExtension;

            // 6. Save file to local filesystem
            targetLocation = uploadPath.resolve(newFilename);

            Files.copy(
                    file.getInputStream(),
                    targetLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // 7. Store relative URL in database
            String imageUrl =
                    "/uploads/product-images/" + newFilename;

            // 8. Create ProductImage entity
            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .isPrimary(
                            request.getIsPrimary() != null
                                    ? request.getIsPrimary()
                                    : false
                    )
                    .createdBy(user)
                    .createdAt(LocalDateTime.now())
                    .modifiedBy(user)
                    .modifiedAt(LocalDateTime.now())
                    .build();

            // 9. Save database record
            ProductImage savedImage =
                    productImageRepository.save(productImage);

            // 10. Return response
            return productImageMapper.toResponseDto(savedImage);

        } catch (Exception ex) {

            /*
             * If file was already saved but DB save failed,
             * delete the file to avoid orphan files.
             */
            if (targetLocation != null) {

                try {
                    Files.deleteIfExists(targetLocation);

                } catch (IOException cleanupException) {

                    System.err.println(
                            "Could not cleanup uploaded file: "
                                    + cleanupException.getMessage());
                }
            }

            throw new RuntimeException(
                    "Could not store image "
                            + file.getOriginalFilename()
                            + ". Please try again!",
                    ex
            );
        }
    }

    /**
     * Validate uploaded image file
     */
    private void validateImageFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required");
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Only image files are allowed");
        }
    }

    /**
     * Get all images belonging to a product
     */
    @Transactional(readOnly = true)
    public List<ProductImageResponseDto> getImagesByProductId(
            Long productId) {

        List<ProductImage> images =
                productImageRepository
                        .findByProductProductId(productId);

        return images.stream()
                .map(productImageMapper::toResponseDto)
                .toList();
    }

    /**
     * Delete image from Local Filesystem and Database
     */
    @Transactional
    public void deleteProductImage(Long imageId) {

        ProductImage productImage =
                productImageRepository.findById(imageId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Product image not found with id: "
                                                + imageId));

        try {

            // 1. Get filename from image URL
            String imageUrl = productImage.getImageUrl();

            String filename =
                    imageUrl.substring(
                            imageUrl.lastIndexOf("/") + 1
                    );

            // 2. Build local file path
            Path filePath = Paths
                    .get(uploadDir)
                    .resolve(filename)
                    .toAbsolutePath()
                    .normalize();

            // 3. Delete physical file
            Files.deleteIfExists(filePath);

        } catch (IOException ex) {

            throw new RuntimeException(
                    "Could not delete image file",
                    ex
            );
        }

        // 4. Delete database record
        productImageRepository.delete(productImage);
    }
}

