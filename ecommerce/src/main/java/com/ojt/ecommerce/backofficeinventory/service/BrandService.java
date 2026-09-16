package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.BrandRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.BrandMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.BrandStatus;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    // 1. CREATE
    @Transactional
    public BrandResponseDto createBrand(BrandRequestDto request) {
        if (brandRepository.existsByBrandName(request.getBrandName())) {
            throw new IllegalArgumentException("Brand with name '" + request.getBrandName() + "' already exists.");
        }

        LocalDateTime now = LocalDateTime.now();

        Brand brand = Brand.builder()
                .brandName(request.getBrandName())
                .brandLogoUrl(request.getBrandLogoUrl())
                .description(request.getDescription())
                .status(request.getStatus())
                .createdAt(now)
                .modifiedAt(now)
                .build();

        return brandMapper.toResponseDto(brandRepository.save(brand));
    }

    // 2. READ ALL (PAGINATED)
    @Transactional(readOnly = true)
    public Page<BrandResponseDto> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(brandMapper::toResponseDto);
    }

    // 3. READ BY ID
    @Transactional(readOnly = true)
    public BrandResponseDto getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));
        return brandMapper.toResponseDto(brand);
    }

    // 4. READ BY STATUS
    @Transactional(readOnly = true)
    public Page<BrandResponseDto> getBrandsByStatus(BrandStatus status, Pageable pageable) {
        return brandRepository.findByStatus(status, pageable)
                .map(brandMapper::toResponseDto);
    }

    // 5. UPDATE
    @Transactional
    public BrandResponseDto updateBrand(Long id, BrandRequestDto request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

        if (brandRepository.existsByBrandNameAndBrandIdNot(request.getBrandName(), id)) {
            throw new IllegalArgumentException("Brand with name '" + request.getBrandName() + "' already exists.");
        }

        brand.setBrandName(request.getBrandName());
        brand.setBrandLogoUrl(request.getBrandLogoUrl());
        brand.setDescription(request.getDescription());
        brand.setStatus(request.getStatus());
        brand.setModifiedAt(LocalDateTime.now());

        return brandMapper.toResponseDto(brandRepository.save(brand));
    }

    // 6. DELETE
    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new EntityNotFoundException("Brand not found: " + id);
        }
        brandRepository.deleteById(id);
    }
}