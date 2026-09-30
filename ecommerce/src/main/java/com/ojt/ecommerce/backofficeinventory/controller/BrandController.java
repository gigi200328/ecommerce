package com.ojt.ecommerce.backofficeinventory.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.BrandRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.backofficeinventory.service.BrandService;
import com.ojt.ecommerce.enums.BrandStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/backoffice/brands")

@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // 1. CREATE - JSON + Optional Logo
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BrandResponseDto> createBrand(
            @Valid @RequestPart("request")
            BrandRequestDto request,

            @RequestPart(value = "file", required = false)
            MultipartFile file) {

        BrandResponseDto response =
                brandService.createBrand(request, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 2. READ ALL - Pagination
    @GetMapping
    public ResponseEntity<Page<BrandResponseDto>> getAllBrands(
            @PageableDefault(
                    size = 20,
                    sort = "brandName"
            ) Pageable pageable) {

        return ResponseEntity.ok(
                brandService.getAllBrands(pageable)
        );
    }

    // 3. READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<BrandResponseDto> getBrandById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                brandService.getBrandById(id)
        );
    }

    // 4. READ BY STATUS
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<BrandResponseDto>> getBrandsByStatus(
            @PathVariable BrandStatus status,

            @PageableDefault(
                    size = 20,
                    sort = "brandName"
            ) Pageable pageable) {

        return ResponseEntity.ok(
                brandService.getBrandsByStatus(status, pageable)
        );
    }

    // 5. UPDATE - JSON + Optional New Logo
    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<BrandResponseDto> updateBrand(
            @PathVariable Long id,

            @Valid @RequestPart("request")
            BrandRequestDto request,

            @RequestPart(value = "file", required = false)
            MultipartFile file) {

        return ResponseEntity.ok(
                brandService.updateBrand(id, request, file)
        );
    }

    // 6. DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(
            @PathVariable Long id) {

        brandService.deleteBrand(id);

        return ResponseEntity.noContent().build();
    }
}