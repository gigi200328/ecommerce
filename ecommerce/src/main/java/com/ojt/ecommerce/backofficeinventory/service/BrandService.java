package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.BrandRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.BrandMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.entity.Brand;
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

    // CREATE
    @Transactional
    public BrandResponseDto createBrand(
            BrandRequestDto request,
            MultipartFile logoFile) {

        if (brandRepository.existsByBrandName(
                request.getBrandName())) {
            throw new IllegalArgumentException(
                    "Brand already exists: " + request.getBrandName()
            );
        }

        String logoUrl = request.getBrandLogoUrl();

        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = fileStorageService.store(
                    logoFile, UploadType.BRAND_LOGO
            );

            registerRollbackCleanup(logoUrl);
        }

        LocalDateTime now = LocalDateTime.now();

        Brand brand = Brand.builder()
                .brandName(request.getBrandName())
                .brandLogoUrl(logoUrl)
                .description(request.getDescription())
                .status(request.getStatus())
                .createdAt(now)
                .modifiedAt(now)
                .build();

        return brandMapper.toResponseDto(
                brandRepository.save(brand)
        );
    }

    // READ ALL
    @Transactional(readOnly = true)
    public Page<BrandResponseDto> getAllBrands(
            Pageable pageable) {

        return brandRepository.findAll(pageable)
                .map(brandMapper::toResponseDto);
    }

    // READ BY ID
    @Transactional(readOnly = true)
    public BrandResponseDto getBrandById(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Brand not found: " + id
                        ));

        return brandMapper.toResponseDto(brand);
    }

    // READ BY STATUS
    @Transactional(readOnly = true)
    public Page<BrandResponseDto> getBrandsByStatus(
            BrandStatus status,
            Pageable pageable) {

        return brandRepository.findByStatus(status, pageable)
                .map(brandMapper::toResponseDto);
    }

    // UPDATE
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
                request.getBrandName(), id)) {
            throw new IllegalArgumentException(
                    "Brand already exists: " + request.getBrandName()
            );
        }

        String oldLogoUrl = brand.getBrandLogoUrl();
        String newLogoUrl = oldLogoUrl;

        if (logoFile != null && !logoFile.isEmpty()) {

            newLogoUrl = fileStorageService.store(
                    logoFile, UploadType.BRAND_LOGO
            );

            registerRollbackCleanup(newLogoUrl);

        } else if (request.getBrandLogoUrl() != null) {

            newLogoUrl = request.getBrandLogoUrl();
        }

        if (!java.util.Objects.equals(
                oldLogoUrl, newLogoUrl)) {
            registerCommitCleanup(oldLogoUrl);
        }

        brand.setBrandName(request.getBrandName());
        brand.setBrandLogoUrl(newLogoUrl);
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus());
        brand.setModifiedAt(LocalDateTime.now());

        return brandMapper.toResponseDto(
                brandRepository.save(brand)
        );
    }

    // DELETE
    @Transactional
    public void deleteBrand(Long id) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Brand not found: " + id
                        ));

        String logoUrl = brand.getBrandLogoUrl();

        brandRepository.delete(brand);

        registerCommitCleanup(logoUrl);
    }

    // Delete newly uploaded file if DB transaction fails.
    private void registerRollbackCleanup(String fileUrl) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCompletion(int status) {

                        if (status != STATUS_COMMITTED) {
                            deleteSafely(fileUrl);
                        }
                    }
                }
        );
    }

    // Delete old file only after DB transaction succeeds.
    private void registerCommitCleanup(String fileUrl) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        deleteSafely(fileUrl);
                    }
                }
        );
    }

    private void deleteSafely(String fileUrl) {

        try {
            fileStorageService.delete(
                    fileUrl, UploadType.BRAND_LOGO
            );
        } catch (RuntimeException ex) {
            System.err.println(
                    "Brand logo cleanup failed: "
                            + ex.getMessage()
            );
        }
    }
}