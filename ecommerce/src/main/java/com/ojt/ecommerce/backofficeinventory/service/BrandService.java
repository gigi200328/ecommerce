
package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.BrandRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.BrandMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandCategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.specification.BrandSpecification;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.BrandCategory;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.BrandStatus;
import com.ojt.ecommerce.enums.UploadType;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandCategoryRepository brandCategoryRepository;
    private final CategoryRepository categoryRepository;
    private final BrandMapper brandMapper;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public BrandResponseDto createBrand(
            BrandRequestDto request,
            MultipartFile logoFile) {

        // -----------------------------------------------------
        // Check duplicate brand name
        // -----------------------------------------------------

        if (brandRepository.existsByBrandName(
                request.getBrandName())) {

            throw new IllegalArgumentException(
                    "Brand already exists: "
                            + request.getBrandName()
            );
        }

        // -----------------------------------------------------
        // Get current logged-in user
        // -----------------------------------------------------

        User currentUser = getCurrentUser();

        // -----------------------------------------------------
        // Handle logo
        // -----------------------------------------------------

        String logoUrl = request.getBrandLogoUrl();

        if (logoFile != null && !logoFile.isEmpty()) {

            logoUrl = fileStorageService.store(
                    logoFile,
                    UploadType.BRAND_LOGO
            );

            // Delete uploaded file if transaction fails
            registerRollbackCleanup(logoUrl);
        }

        // -----------------------------------------------------
        // Create Brand
        // -----------------------------------------------------

        LocalDateTime now = LocalDateTime.now();

        Brand brand = Brand.builder()
                .brandName(request.getBrandName())
                .brandLogoUrl(logoUrl)
                .description(request.getDescription())
                .status(request.getStatus())

                // Audit fields
                .createdBy(currentUser)
                .createdAt(now)
                .modifiedBy(currentUser)
                .modifiedAt(now)

                .build();

        Brand savedBrand = brandRepository.save(brand);

        // -----------------------------------------------------
        // Save Brand Categories
        // -----------------------------------------------------

        saveBrandCategories(
                savedBrand,
                request.getCategoryIds()
        );

        return brandMapper.toResponseDto(savedBrand);
    }

    // =========================================================
    // READ ALL + SEARCH + STATUS FILTER
    // =========================================================

    @Transactional(readOnly = true)
    public Page<BrandResponseDto> searchBrands(
            String keyword,
            BrandStatus status,
            Pageable pageable) {

        Specification<Brand> specification =
                BrandSpecification.search(
                        keyword,
                        status
                );

        return brandRepository
                .findAll(specification, pageable)
                .map(brandMapper::toResponseDto);
    }

    // =========================================================
    // READ ALL
    // =========================================================

    @Transactional(readOnly = true)
    public Page<BrandResponseDto> getAllBrands(
            Pageable pageable) {

        return brandRepository
                .findAll(pageable)
                .map(brandMapper::toResponseDto);
    }

    // =========================================================
    // READ BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public BrandResponseDto getBrandById(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Brand not found: " + id
                        ));

        return brandMapper.toResponseDto(brand);
    }

    // =========================================================
    // READ BY STATUS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<BrandResponseDto> getBrandsByStatus(
            BrandStatus status,
            Pageable pageable) {

        return brandRepository
                .findByStatus(status, pageable)
                .map(brandMapper::toResponseDto);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public BrandResponseDto updateBrand(
            Long id,
            BrandRequestDto request,
            MultipartFile logoFile) {

        // -----------------------------------------------------
        // Find brand
        // -----------------------------------------------------

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Brand not found: " + id
                        ));

        // -----------------------------------------------------
        // Check duplicate brand name
        // -----------------------------------------------------

        if (brandRepository.existsByBrandNameAndBrandIdNot(
                request.getBrandName(),
                id)) {

            throw new IllegalArgumentException(
                    "Brand already exists: "
                            + request.getBrandName()
            );
        }

        // -----------------------------------------------------
        // Get current logged-in user
        // -----------------------------------------------------

        User currentUser = getCurrentUser();

        // -----------------------------------------------------
        // Handle logo
        // -----------------------------------------------------

        String oldLogoUrl = brand.getBrandLogoUrl();
        String newLogoUrl = oldLogoUrl;

        if (logoFile != null && !logoFile.isEmpty()) {

            newLogoUrl = fileStorageService.store(
                    logoFile,
                    UploadType.BRAND_LOGO
            );

            // Delete new logo if transaction fails
            registerRollbackCleanup(newLogoUrl);

        } else if (request.getBrandLogoUrl() != null) {

            newLogoUrl = request.getBrandLogoUrl();
        }

        // -----------------------------------------------------
        // Delete old logo after successful transaction
        // -----------------------------------------------------

        if (!Objects.equals(
                oldLogoUrl,
                newLogoUrl)) {

            registerCommitCleanup(oldLogoUrl);
        }

        // -----------------------------------------------------
        // Update Brand
        // -----------------------------------------------------

        brand.setBrandName(
                request.getBrandName()
        );

        brand.setBrandLogoUrl(
                newLogoUrl
        );

        brand.setDescription(
                request.getDescription()
        );

        brand.setStatus(
                request.getStatus()
        );

        // Audit fields
        brand.setModifiedBy(currentUser);
        brand.setModifiedAt(LocalDateTime.now());

        Brand savedBrand = brandRepository.save(brand);

        // -----------------------------------------------------
        // Update Brand Categories
        // -----------------------------------------------------

        // Delete old relationships first
        brandCategoryRepository.deleteByBrandBrandId(id);

        // Save new relationships
        saveBrandCategories(
                savedBrand,
                request.getCategoryIds()
        );

        return brandMapper.toResponseDto(savedBrand);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void deleteBrand(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Brand not found: " + id
                        ));

        String logoUrl = brand.getBrandLogoUrl();

        // -----------------------------------------------------
        // Delete BrandCategory relationships first
        // -----------------------------------------------------

        brandCategoryRepository.deleteByBrandBrandId(id);

        // -----------------------------------------------------
        // Delete Brand
        // -----------------------------------------------------

        brandRepository.delete(brand);

        // -----------------------------------------------------
        // Delete logo after successful DB transaction
        // -----------------------------------------------------

        registerCommitCleanup(logoUrl);
    }

    // =========================================================
    // SAVE BRAND CATEGORIES
    // =========================================================

    private void saveBrandCategories(
            Brand brand,
            java.util.List<Long> categoryIds) {

        if (categoryIds == null || categoryIds.isEmpty()) {
            return;
        }

        for (Long categoryId : categoryIds) {

            if (categoryId == null) {
                continue;
            }

            Category category = categoryRepository
                    .findById(categoryId)
                    .orElseThrow(() ->
                            new EntityNotFoundException(
                                    "Category not found: "
                                            + categoryId
                            ));

            BrandCategory brandCategory =
                    BrandCategory.builder()
                            .brand(brand)
                            .category(category)
                            .build();

            brandCategoryRepository.save(
                    brandCategory
            );
        }
    }

    // =========================================================
    // GET CURRENT LOGGED-IN USER
    // =========================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new IllegalStateException(
                    "User is not authenticated"
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found: " + email
                        ));
    }

    // =========================================================
    // ROLLBACK FILE CLEANUP
    // =========================================================

    private void registerRollbackCleanup(
            String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCompletion(
                                    int status) {

                                if (status != STATUS_COMMITTED) {
                                    deleteSafely(fileUrl);
                                }
                            }
                        }
                );
    }

    // =========================================================
    // COMMIT FILE CLEANUP
    // =========================================================

    private void registerCommitCleanup(
            String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {
                                deleteSafely(fileUrl);
                            }
                        }
                );
    }

    // =========================================================
    // SAFE FILE DELETE
    // =========================================================

    private void deleteSafely(String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {

            fileStorageService.delete(
                    fileUrl,
                    UploadType.BRAND_LOGO
            );

        } catch (RuntimeException ex) {

            System.err.println(
                    "Brand logo cleanup failed: "
                            + ex.getMessage()
            );
        }
    }
}

