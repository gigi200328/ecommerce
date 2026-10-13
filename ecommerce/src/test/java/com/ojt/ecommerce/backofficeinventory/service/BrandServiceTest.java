
package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.ojt.ecommerce.backofficeinventory.dto.BrandRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.BrandResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.BrandMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandCategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;

import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.entity.BrandCategory;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.User;

import com.ojt.ecommerce.enums.BrandStatus;
import com.ojt.ecommerce.enums.UploadType;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private BrandCategoryRepository brandCategoryRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandMapper brandMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BrandService brandService;

    private User user;
    private BrandResponseDto responseDto;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        responseDto = mock(BrandResponseDto.class);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        lenient()
                .when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));

        // BrandService uses transaction synchronization callbacks.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private Brand brand(
            Long id,
            String name,
            BrandStatus status,
            String logoUrl
    ) {
        Brand brand = Brand.builder()
                .brandName(name)
                .brandLogoUrl(logoUrl)
                .description("Test Description")
                .status(status)
                .createdBy(user)
                .modifiedBy(user)
                .build();

        brand.setBrandId(id);
        return brand;
    }

    private Category category(Long id, String name) {
        Category category = Category.builder()
                .categoryName(name)
                .build();

        category.setCategoryId(id);
        return category;
    }

    private BrandRequestDto request(
            String name,
            String description,
            BrandStatus status,
            List<Long> categoryIds
    ) {
        BrandRequestDto request = new BrandRequestDto();
        request.setBrandName(name);
        request.setDescription(description);
        request.setStatus(status);
        request.setCategoryIds(categoryIds);

        return request;
    }

    // =========================================================
    // CREATE BRAND TESTS
    // =========================================================

    @Test
    void createBrand_success_withoutFile() {
        BrandRequestDto request = request(
                "Nike",
                "Sports brand",
                BrandStatus.ACTIVE,
                List.of(1L)
        );

        Brand savedBrand = brand(
                1L, "Nike", BrandStatus.ACTIVE, null
        );

        Category category = category(1L, "Sports");

        when(brandRepository.existsByBrandName("Nike"))
                .thenReturn(false);
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(brandRepository.save(any(Brand.class)))
                .thenReturn(savedBrand);
        when(brandMapper.toResponseDto(savedBrand))
                .thenReturn(responseDto);

        BrandResponseDto result =
                brandService.createBrand(request, null);

        assertSame(responseDto, result);

        verify(brandRepository).save(any(Brand.class));
        verify(brandCategoryRepository).save(
                any(BrandCategory.class)
        );
        verify(fileStorageService, never()).store(
                any(MultipartFile.class),
                eq(UploadType.BRAND_LOGO)
        );
    }

    @Test
    void createBrand_whenNameExists_throwsException() {
        BrandRequestDto request = request(
                "Nike",
                "Sports brand",
                BrandStatus.ACTIVE,
                null
        );

        when(brandRepository.existsByBrandName("Nike"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> brandService.createBrand(request, null)
        );

        verify(brandRepository, never()).save(any(Brand.class));
        verify(userRepository, never())
                .findByEmail(any(String.class));
    }

    @Test
    void createBrand_withLogoFile_storesFileAndSavesBrand() {
        BrandRequestDto request = request(
                "Nike",
                "Sports brand",
                BrandStatus.ACTIVE,
                null
        );

        MultipartFile file = mock(MultipartFile.class);

        Brand savedBrand = brand(
                1L,
                "Nike",
                BrandStatus.ACTIVE,
                "nike-logo.png"
        );

        when(brandRepository.existsByBrandName("Nike"))
                .thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(fileStorageService.store(
                file,
                UploadType.BRAND_LOGO
        )).thenReturn("nike-logo.png");
        when(brandRepository.save(any(Brand.class)))
                .thenReturn(savedBrand);
        when(brandMapper.toResponseDto(savedBrand))
                .thenReturn(responseDto);

        BrandResponseDto result =
                brandService.createBrand(request, file);

        assertSame(responseDto, result);

        verify(fileStorageService).store(
                file,
                UploadType.BRAND_LOGO
        );

        verify(brandRepository).save(any(Brand.class));

    }

    // =========================================================
    // SEARCH AND READ BRAND TESTS
    // =========================================================

    @Test
    void searchBrands_returnsPagedResult() {
        Brand item = brand(
                1L, "Nike", BrandStatus.ACTIVE, null
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Brand> page = new PageImpl<>(List.of(item));

        when(brandRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        when(brandMapper.toResponseDto(item))
                .thenReturn(responseDto);

        Page<BrandResponseDto> result =
                brandService.searchBrands(
                        "Nike",
                        BrandStatus.ACTIVE,
                        pageable
                );

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));

        verify(brandRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    void getAllBrands_returnsPagedResult() {
        Brand item = brand(
                1L, "Nike", BrandStatus.ACTIVE, null
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Brand> page = new PageImpl<>(List.of(item));

        when(brandRepository.findAll(pageable))
                .thenReturn(page);
        when(brandMapper.toResponseDto(item))
                .thenReturn(responseDto);

        Page<BrandResponseDto> result =
                brandService.getAllBrands(pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));
    }

    @Test
    void getBrandById_whenFound_returnsDto() {
        Brand item = brand(
                1L, "Nike", BrandStatus.ACTIVE, null
        );

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(item));
        when(brandMapper.toResponseDto(item))
                .thenReturn(responseDto);

        assertSame(
                responseDto,
                brandService.getBrandById(1L)
        );
    }

    @Test
    void getBrandById_whenMissing_throwsException() {
        when(brandRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> brandService.getBrandById(99L)
        );
    }

    @Test
    void getBrandsByStatus_returnsPagedResult() {
        Brand item = brand(
                1L, "Nike", BrandStatus.ACTIVE, null
        );

        Pageable pageable = PageRequest.of(0, 10);
        Page<Brand> page = new PageImpl<>(List.of(item));

        when(brandRepository.findByStatus(
                BrandStatus.ACTIVE,
                pageable
        )).thenReturn(page);

        when(brandMapper.toResponseDto(item))
                .thenReturn(responseDto);

        Page<BrandResponseDto> result =
                brandService.getBrandsByStatus(
                        BrandStatus.ACTIVE,
                        pageable
                );

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));
    }

    // =========================================================
    // UPDATE BRAND TESTS
    // =========================================================

    @Test
    void updateBrand_success() {
        Brand existingBrand = brand(
                1L, "Nike", BrandStatus.ACTIVE, "old-logo.png"
        );

        BrandRequestDto request = request(
                "Nike Updated",
                "New desc",
                BrandStatus.ACTIVE,
                List.of(1L)
        );

        Category category = category(1L, "Sports");

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(existingBrand));
        when(brandRepository.existsByBrandNameAndBrandIdNot(
                "Nike Updated",
                1L
        )).thenReturn(false);
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));
        when(brandRepository.save(existingBrand))
                .thenReturn(existingBrand);
        when(brandMapper.toResponseDto(existingBrand))
                .thenReturn(responseDto);

        BrandResponseDto result =
                brandService.updateBrand(1L, request, null);

        assertSame(responseDto, result);
        assertEquals("Nike Updated", existingBrand.getBrandName());
        assertEquals("New desc", existingBrand.getDescription());
        assertSame(user, existingBrand.getModifiedBy());
        assertNotNull(existingBrand.getModifiedAt());

        verify(brandCategoryRepository)
                .deleteByBrandBrandId(1L);
        verify(brandCategoryRepository).save(
                any(BrandCategory.class)
        );
    }

    @Test
    void updateBrand_whenDuplicateName_throwsException() {
        Brand existingBrand = brand(
                1L, "Nike", BrandStatus.ACTIVE, null
        );

        BrandRequestDto request = request(
                "Adidas",
                "Desc",
                BrandStatus.ACTIVE,
                null
        );

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(existingBrand));
        when(brandRepository.existsByBrandNameAndBrandIdNot(
                "Adidas",
                1L
        )).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> brandService.updateBrand(1L, request, null)
        );

        verify(brandRepository, never()).save(any(Brand.class));
    }

    @Test
    void updateBrand_whenMissing_throwsException() {
        when(brandRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> brandService.updateBrand(
                        99L,
                        request(
                                "Name",
                                "Desc",
                                BrandStatus.ACTIVE,
                                null
                        ),
                        null
                )
        );
    }

    // =========================================================
    // DELETE BRAND TESTS
    // =========================================================

    @Test
    void deleteBrand_success() {
        Brand item = brand(
                1L, "Nike", BrandStatus.ACTIVE, "logo.png"
        );

        when(brandRepository.findById(1L))
                .thenReturn(Optional.of(item));

        assertDoesNotThrow(
                () -> brandService.deleteBrand(1L)
        );

        verify(brandCategoryRepository)
                .deleteByBrandBrandId(1L);
        verify(brandRepository).delete(item);
    }

    @Test
    void deleteBrand_whenMissing_throwsException() {
        when(brandRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> brandService.deleteBrand(99L)
        );

        verify(brandCategoryRepository, never())
                .deleteByBrandBrandId(99L);
        verify(brandRepository, never())
                .delete(any(Brand.class));
    }
}
