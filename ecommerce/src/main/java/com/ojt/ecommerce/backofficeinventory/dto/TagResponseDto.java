package com.ojt.ecommerce.backofficeinventory.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponseDto {

    private Long tagId;
    private String tagName;
    private LocalDateTime createdAt;
}