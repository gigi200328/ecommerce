package com.ojt.ecommerce.backofficeinventory.mapper;

import com.ojt.ecommerce.backofficeinventory.dto.TagResponseDto;
import com.ojt.ecommerce.entity.Tag;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagResponseDto toResponseDto(Tag tag) {
        if (tag == null) {
            return null;
        }

        return TagResponseDto.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}