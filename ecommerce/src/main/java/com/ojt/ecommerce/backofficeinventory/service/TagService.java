package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.TagRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.TagResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.TagMapper;
import com.ojt.ecommerce.backofficeinventory.repository.TagRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;

import com.ojt.ecommerce.entity.Tag;
import com.ojt.ecommerce.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final UserRepository userRepository;

    /**
     * Get currently logged-in user.
     */
    private User getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "User is not authenticated."
            );
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Current user not found: " + email
                        )
                );
    }

    /**
     * Create new tag.
     */
    @Transactional
    public TagResponseDto createTag(TagRequestDto request) {

        String tagName = request.getTagName().trim();

        // Check duplicate tag name
        if (tagRepository.existsByTagNameIgnoreCase(tagName)) {

            throw new RuntimeException(
                    "Tag name already exists: " + tagName
            );
        }

        User currentUser = getCurrentUser();

        LocalDateTime now = LocalDateTime.now();

        Tag tag = Tag.builder()
                .tagName(tagName)

                // Created information
                .createdAt(now)
                .createdBy(currentUser)

                // No modification yet
                .modifiedAt(null)
                .modifiedBy(null)

                .build();

        Tag savedTag = tagRepository.save(tag);

        return tagMapper.toResponseDto(savedTag);
    }

    /**
     * Get all tags with optional search.
     *
     * Example:
     * search = "phone"
     */
    @Transactional(readOnly = true)
    public Page<TagResponseDto> getAllTags(
            String search,
            Pageable pageable
    ) {

        Page<Tag> tagPage;

        if (search == null || search.trim().isEmpty()) {

            tagPage = tagRepository.findAll(pageable);

        } else {

            String keyword = search.trim();

            tagPage =
                    tagRepository
                            .findByTagNameContainingIgnoreCase(
                                    keyword,
                                    pageable
                            );
        }

        return tagPage.map(tagMapper::toResponseDto);
    }

    /**
     * Get tag by ID.
     */
    @Transactional(readOnly = true)
    public TagResponseDto getTagById(Long id) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tag not found with id: " + id
                        )
                );

        return tagMapper.toResponseDto(tag);
    }

    /**
     * Update tag.
     */
    @Transactional
    public TagResponseDto updateTag(
            Long id,
            TagRequestDto request
    ) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tag not found with id: " + id
                        )
                );

        String tagName = request.getTagName().trim();

        /*
         * Check duplicate only when
         * the name is changed.
         */
        if (!tag.getTagName().equalsIgnoreCase(tagName)
                && tagRepository.existsByTagNameIgnoreCase(tagName)) {

            throw new RuntimeException(
                    "Tag name already exists: " + tagName
            );
        }

        User currentUser = getCurrentUser();

        tag.setTagName(tagName);

        // Update modification information
        tag.setModifiedBy(currentUser);
        tag.setModifiedAt(LocalDateTime.now());

        Tag updatedTag = tagRepository.save(tag);

        return tagMapper.toResponseDto(updatedTag);
    }

    /**
     * Delete tag.
     */
    @Transactional
    public void deleteTag(Long id) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tag not found with id: " + id
                        )
                );

        /*
         * Delete tag.
         *
         * If ProductTag records reference this tag,
         * delete those associations before deleting the tag.
         */
        tagRepository.delete(tag);
    }
}