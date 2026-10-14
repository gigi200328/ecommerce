
package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ojt.ecommerce.backofficeinventory.dto.CategoryRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.CategoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.CategoryMapper;
import com.ojt.ecommerce.backofficeinventory.repository.BrandCategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.User;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandCategoryRepository brandCategoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private User user;
    private CategoryResponseDto response;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        response = mock(CategoryResponseDto.class);

        // Set authenticated user
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@example.com",
                        "password",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        // Lenient avoids unnecessary-stubbing errors in tests
        // that do not call getCurrentUser().
        lenient()
                .when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Category category(Long id, String name, Category parent) {
        Category category = Category.builder()
                .categoryName(name)
                .parent(parent)
                .createdBy(user)
                .modifiedBy(user)
                .build();

        category.setCategoryId(id);
        return category;
    }

    private CategoryRequestDto request(
            String name,
            String description,
            Long parentId
    ) {
        CategoryRequestDto request = new CategoryRequestDto();
        request.setCategoryName(name);
        request.setDescription(description);
        request.setParentId(parentId);
        return request;
    }

    // CREATE CATEGORY

    @Test
    void createCategory_withoutParent_savesRootCategory() {
        CategoryRequestDto request =
                request("Electronics", "Devices", null);

        Category saved = category(1L, "Electronics", null);

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(saved);
        when(categoryMapper.toResponseDto(saved))
                .thenReturn(response);

        CategoryResponseDto result =
                categoryService.createCategory(request);

        assertSame(response, result);

        verify(categoryRepository).save(argThat(c ->
                "Electronics".equals(c.getCategoryName())
                && "Devices".equals(c.getDescription())
                && c.getParent() == null
                && c.getCreatedBy() == user
                && c.getModifiedBy() == user
                && c.getCreatedAt() != null
                && c.getModifiedAt() != null
        ));
    }

    @Test
    void createCategory_withParent_savesChildCategory() {
        Category parent = category(1L, "Electronics", null);

        CategoryRequestDto request =
                request("Phones", "Mobile phones", 1L);

        Category saved = category(2L, "Phones", parent);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(saved);
        when(categoryMapper.toResponseDto(saved))
                .thenReturn(response);

        assertSame(response, categoryService.createCategory(request));

        verify(categoryRepository).save(argThat(c ->
                c.getParent() == parent
        ));
    }

    @Test
    void createCategory_whenParentDoesNotExist_throwsException() {
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.createCategory(
                        request("Phones", "Mobile phones", 99L)
                )
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void createCategory_whenParentIsAlreadyLevelThree_rejectsChild() {
        Category level1 = category(1L, "Level 1", null);
        Category level2 = category(2L, "Level 2", level1);
        Category level3 = category(3L, "Level 3", level2);

        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(level3));

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory(
                        request("Level 4", "", 3L)
                )
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }


    @Test
    void createCategory_whenCategoryNameAlreadyExists_allowsDuplicateName() {
        CategoryRequestDto request =
                request("Electronics", "Another electronics category", null);
        Category saved = category(2L, "Electronics", null);

        when(categoryRepository.save(any(Category.class)))
                .thenReturn(saved);
        when(categoryMapper.toResponseDto(saved))
                .thenReturn(response);

        CategoryResponseDto result = categoryService.createCategory(request);

        assertSame(response, result);
        verify(categoryRepository).save(argThat(c ->
                "Electronics".equals(c.getCategoryName())
                && c.getParent() == null
        ));
    }

    // GET ALL CATEGORIES

    @Test
    void getAllCategories_mapsPage() {
        Category item = category(1L, "Electronics", null);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(item));

        when(categoryRepository.findAll(pageable))
                .thenReturn(page);
        when(categoryMapper.toResponseDto(item))
                .thenReturn(response);

        Page<CategoryResponseDto> result =
                categoryService.getAllCategories(pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));
    }

    // GET CATEGORY TREE

    @Test
    void getCategoryTree_returnsMapperTree() {
        List<Category> categories =
                List.of(category(1L, "Electronics", null));

        List<CategoryResponseDto> expected = List.of(response);

        when(categoryRepository.findAll())
                .thenReturn(categories);
        when(categoryMapper.toTreeDtoList(categories))
                .thenReturn(expected);

        assertSame(expected, categoryService.getCategoryTree());
    }

    // GET CATEGORY BY ID

    @Test
    void getCategoryById_whenFound_returnsDto() {
        Category item = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(item));
        when(categoryMapper.toResponseDto(item))
                .thenReturn(response);

        assertSame(response, categoryService.getCategoryById(1L));
    }

    @Test
    void getCategoryById_whenMissing_throwsException() {
        when(categoryRepository.findById(404L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.getCategoryById(404L)
        );
    }

    // UPDATE CATEGORY

    @Test
    void updateCategory_updatesFieldsAndParent() {
        Category item = category(2L, "Old name", null);
        Category parent = category(1L, "Electronics", null);

        CategoryRequestDto request =
                request("Phones", "Updated description", 1L);

        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(item));
        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(parent));
        when(categoryRepository.save(item))
                .thenReturn(item);
        when(categoryMapper.toResponseDto(item))
                .thenReturn(response);

        assertSame(
                response,
                categoryService.updateCategory(2L, request)
        );

        assertEquals("Phones", item.getCategoryName());
        assertEquals("Updated description", item.getDescription());
        assertSame(parent, item.getParent());
        assertSame(user, item.getModifiedBy());
        assertNotNull(item.getModifiedAt());
    }

    @Test
    void updateCategory_whenCategoryMissing_throwsException() {
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.updateCategory(
                        99L,
                        request("Name", "Desc", null)
                )
        );
    }

    @Test
    void updateCategory_cannotSetItselfAsParent() {
        Category item = category(2L, "Phones", null);

        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(item));

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(
                        2L,
                        request("Phones", "", 2L)
                )
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void updateCategory_cannotSetDescendantAsParent() {
        Category root = category(1L, "Root", null);
        Category child = category(2L, "Child", root);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(root));
        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(child));

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(
                        1L,
                        request("Root", "", 2L)
                )
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void updateCategory_rejectsMoveThatExceedsMaximumDepth() {
        Category targetRoot =
                category(1L, "Target root", null);

        Category targetLevel2 =
                category(2L, "Target level 2", targetRoot);

        Category targetLevel3 =
                category(3L, "Target level 3", targetLevel2);

        Category moving = category(4L, "Moving", null);
        Category child = category(5L, "Child", moving);

        moving.setChildren(Set.of(child));

        when(categoryRepository.findById(4L))
                .thenReturn(Optional.of(moving));
        when(categoryRepository.findById(3L))
                .thenReturn(Optional.of(targetLevel3));

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.updateCategory(
                        4L,
                        request("Moving", "", 3L)
                )
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    @Test
    void updateCategory_withNullParent_makesCategoryRoot() {
        Category oldParent =
                category(1L, "Electronics", null);

        Category item =
                category(2L, "Phones", oldParent);

        when(categoryRepository.findById(2L))
                .thenReturn(Optional.of(item));
        when(categoryRepository.save(item))
                .thenReturn(item);
        when(categoryMapper.toResponseDto(item))
                .thenReturn(response);

        categoryService.updateCategory(
                2L,
                request("Phones updated", "Desc", null)
        );

        assertNull(item.getParent());
        assertEquals("Phones updated", item.getCategoryName());
    }

    // DELETE CATEGORY

    @Test
    void deleteCategory_deletesCategoryWithoutChildrenOrBrandLinks() {
        Category item = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(item));
        when(categoryRepository.existsByParentCategoryId(1L))
                .thenReturn(false);
        when(brandCategoryRepository.existsByCategoryCategoryId(1L))
                .thenReturn(false);

        assertDoesNotThrow(
                () -> categoryService.deleteCategory(1L)
        );

        verify(categoryRepository).delete(item);
    }

    @Test
    void deleteCategory_whenCategoryMissing_throwsException() {
        when(categoryRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.deleteCategory(99L)
        );

        verify(categoryRepository, never())
                .delete(any(Category.class));
    }

    @Test
    void deleteCategory_withChildren_isRejected() {
        Category item = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(item));
        when(categoryRepository.existsByParentCategoryId(1L))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.deleteCategory(1L)
        );

        verify(brandCategoryRepository, never())
                .existsByCategoryCategoryId(anyLong());

        verify(categoryRepository, never())
                .delete(any(Category.class));
    }

    @Test
    void deleteCategory_associatedWithBrand_isRejected() {
        Category item = category(1L, "Electronics", null);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(item));
        when(categoryRepository.existsByParentCategoryId(1L))
                .thenReturn(false);
        when(brandCategoryRepository.existsByCategoryCategoryId(1L))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.deleteCategory(1L)
        );

        verify(categoryRepository, never())
                .delete(any(Category.class));
    }

    // SECURITY

    @Test
    void createCategory_whenUnauthenticated_throwsException() {
        SecurityContextHolder.clearContext();

        assertThrows(
                IllegalStateException.class,
                () -> categoryService.createCategory(
                        request("Electronics", "", null)
                )
        );

        verify(userRepository, never())
                .findByEmail(anyString());
    }

    @Test
    void createCategory_whenAuthenticatedUserIsMissing_throwsException() {
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.createCategory(
                        request("Electronics", "", null)
                )
        );

        verify(categoryRepository, never())
                .save(any(Category.class));
    }

    // SEARCH CATEGORY

    @Test
    void searchCategories_withKeyword_returnsPagedResult() {
        Category item = category(1L, "Electronics", null);
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(item));

        when(categoryRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(page);

        when(categoryMapper.toResponseDto(item))
                .thenReturn(response);

        Page<CategoryResponseDto> result =
                categoryService.searchCategories("Elec", pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));

        verify(categoryRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );

        verify(categoryMapper).toResponseDto(item);
    }
}
