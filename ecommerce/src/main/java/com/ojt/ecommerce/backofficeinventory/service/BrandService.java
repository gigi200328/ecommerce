
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
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.specification.BrandSpecification;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.enums.BrandStatus;
import com.ojt.ecommerce.enums.UploadType;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
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

        if (brandRepository.existsByBrandName(
                request.getBrandName())) {

            throw new IllegalArgumentException(
                    "Brand already exists: "
                            + request.getBrandName()
            );
        }

        // Get currently logged-in user
        User currentUser = getCurrentUser();

        String logoUrl = request.getBrandLogoUrl();

        if (logoFile != null && !logoFile.isEmpty()) {

            logoUrl = fileStorageService.store(
                    logoFile,
                    UploadType.BRAND_LOGO
            );

            // Delete uploaded file if DB transaction fails
            registerRollbackCleanup(logoUrl);
        }

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

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Brand not found: " + id
                        ));

        if (brandRepository.existsByBrandNameAndBrandIdNot(
                request.getBrandName(),
                id)) {

            throw new IllegalArgumentException(
                    "Brand already exists: "
                            + request.getBrandName()
            );
        }

        // Get currently logged-in user
        User currentUser = getCurrentUser();

        String oldLogoUrl = brand.getBrandLogoUrl();
        String newLogoUrl = oldLogoUrl;

        // -----------------------------------------------------
        // Handle new logo
        // -----------------------------------------------------

        if (logoFile != null && !logoFile.isEmpty()) {

            newLogoUrl = fileStorageService.store(
                    logoFile,
                    UploadType.BRAND_LOGO
            );

            // Delete newly uploaded file if DB transaction fails
            registerRollbackCleanup(newLogoUrl);

        } else if (request.getBrandLogoUrl() != null) {

            newLogoUrl = request.getBrandLogoUrl();
        }

        // -----------------------------------------------------
        // Delete old logo only after successful DB transaction
        // -----------------------------------------------------

        if (!Objects.equals(
                oldLogoUrl,
                newLogoUrl)) {

            registerCommitCleanup(oldLogoUrl);
        }

        // -----------------------------------------------------
        // Update brand fields
        // -----------------------------------------------------

        brand.setBrandName(request.getBrandName());
        brand.setBrandLogoUrl(newLogoUrl);
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus());

        // Audit fields
        brand.setModifiedBy(currentUser);
        brand.setModifiedAt(LocalDateTime.now());

        Brand savedBrand = brandRepository.save(brand);

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

        brandRepository.delete(brand);

        // Delete logo only after DB transaction succeeds
        registerCommitCleanup(logoUrl);
    }

    // =========================================================
    // GET CURRENT LOGGED-IN USER
    // =========================================================

    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName() == null ||
                authentication.getName().isBlank()) {

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

