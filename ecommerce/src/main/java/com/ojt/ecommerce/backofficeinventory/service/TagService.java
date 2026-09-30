
package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.TagRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.TagResponseDto;
import com.ojt.ecommerce.backofficeinventory.mapper.TagMapper;
import com.ojt.ecommerce.backofficeinventory.repository.TagRepository;
import com.ojt.ecommerce.entity.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Transactional
    public TagResponseDto createTag(TagRequestDto request) {

        String tagName = request.getTagName().trim();

        // Check duplicate tag name
        if (tagRepository.existsByTagNameIgnoreCase(tagName)) {
            throw new RuntimeException(
                    "Tag name already exists: " + tagName
            );
        }

        Tag tag = Tag.builder()
                .tagName(tagName)
                .createdAt(LocalDateTime.now())
                .build();

        Tag savedTag = tagRepository.save(tag);

        return tagMapper.toResponseDto(savedTag);
    }

    @Transactional(readOnly = true)
    public Page<TagResponseDto> getAllTags(Pageable pageable) {

        return tagRepository.findAll(pageable)
                .map(tagMapper::toResponseDto);
    }

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

    @Transactional
    public TagResponseDto updateTag(
            Long id,
            TagRequestDto request) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tag not found with id: " + id
                        )
                );

        String tagName = request.getTagName().trim();

        /*
         * Check duplicate only when the name is changed.
         */
        if (!tag.getTagName().equalsIgnoreCase(tagName)
                && tagRepository.existsByTagNameIgnoreCase(tagName)) {

            throw new RuntimeException(
                    "Tag name already exists: " + tagName
            );
        }

        tag.setTagName(tagName);

        Tag updatedTag = tagRepository.save(tag);

        return tagMapper.toResponseDto(updatedTag);
    }

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
         * ProductTagRepository should delete those associations
         * before deleting the tag.
         */
        tagRepository.delete(tag);
    }
}

