package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ojt.ecommerce.backofficeinventory.dto.TagRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.TagResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.TagMapper;
import com.ojt.ecommerce.backofficeinventory.repository.TagRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Tag;
import com.ojt.ecommerce.entity.User;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TagMapper tagMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TagService tagService;

    private User user;
    private TagResponseDto responseDto;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        responseDto = mock(TagResponseDto.class);

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
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Tag tag(Long id, String name) {
        Tag tag = Tag.builder()
                .tagName(name)
                .createdBy(user)
                .build();
        tag.setTagId(id);
        return tag;
    }

    private TagRequestDto request(String name) {
        TagRequestDto request = new TagRequestDto();
        request.setTagName(name);
        return request;
    }

    // =========================================================
    // CREATE TAG TESTS
    // =========================================================

    @Test
    void createTag_success() {
        TagRequestDto request = request("Electronics");
        Tag savedTag = tag(1L, "Electronics");

        when(tagRepository.existsByTagNameIgnoreCase("Electronics")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);
        when(tagMapper.toResponseDto(savedTag)).thenReturn(responseDto);

        TagResponseDto result = tagService.createTag(request);

        assertSame(responseDto, result);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void createTag_whenNameExists_throwsException() {
        TagRequestDto request = request("Electronics");

        when(tagRepository.existsByTagNameIgnoreCase("Electronics")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> tagService.createTag(request));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    // =========================================================
    // GET ALL TAGS & SEARCH TESTS
    // =========================================================

    @Test
    void getAllTags_withoutSearch_returnsPagedResult() {
        Tag tag = tag(1L, "Electronics");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tag> page = new PageImpl<>(List.of(tag));

        when(tagRepository.findAll(pageable)).thenReturn(page);
        when(tagMapper.toResponseDto(tag)).thenReturn(responseDto);

        Page<TagResponseDto> result = tagService.getAllTags(null, pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));
    }

    @Test
    void getAllTags_withSearch_returnsPagedResult() {
        Tag tag = tag(1L, "Electronics");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tag> page = new PageImpl<>(List.of(tag));

        when(tagRepository.findByTagNameContainingIgnoreCase(eq("Electro"), eq(pageable)))
                .thenReturn(page);
        when(tagMapper.toResponseDto(tag)).thenReturn(responseDto);

        Page<TagResponseDto> result = tagService.getAllTags(" Electro ", pageable);

        assertEquals(1, result.getTotalElements());
        assertSame(responseDto, result.getContent().get(0));
    }

    // =========================================================
    // GET TAG BY ID TESTS
    // =========================================================

    @Test
    void getTagById_success() {
        Tag tag = tag(1L, "Electronics");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagMapper.toResponseDto(tag)).thenReturn(responseDto);

        assertSame(responseDto, tagService.getTagById(1L));
    }

    @Test
    void getTagById_whenMissing_throwsException() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tagService.getTagById(99L));
    }

    // =========================================================
    // UPDATE TAG TESTS
    // =========================================================

    @Test
    void updateTag_success() {
        Tag existingTag = tag(1L, "Electronics");
        TagRequestDto request = request("Home Appliances");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(existingTag));
        when(tagRepository.existsByTagNameIgnoreCase("Home Appliances")).thenReturn(false);
        when(tagRepository.save(existingTag)).thenReturn(existingTag);
        when(tagMapper.toResponseDto(existingTag)).thenReturn(responseDto);

        TagResponseDto result = tagService.updateTag(1L, request);

        assertSame(responseDto, result);
        assertEquals("Home Appliances", existingTag.getTagName());
        verify(tagRepository).save(existingTag);
    }

    @Test
    void updateTag_whenDuplicateName_throwsException() {
        Tag existingTag = tag(1L, "Electronics");
        TagRequestDto request = request("Fashion");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(existingTag));
        when(tagRepository.existsByTagNameIgnoreCase("Fashion")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> tagService.updateTag(1L, request));
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void updateTag_whenMissing_throwsException() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tagService.updateTag(99L, request("Test")));
    }

    // =========================================================
    // DELETE TAG TESTS
    // =========================================================

    @Test
    void deleteTag_success() {
        Tag tag = tag(1L, "Electronics");

        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        assertDoesNotThrow(() -> tagService.deleteTag(1L));

        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteTag_whenMissing_throwsException() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> tagService.deleteTag(99L));
        verify(tagRepository, never()).delete(any(Tag.class));
    }
}