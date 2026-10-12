
package com.ojt.ecommerce.backofficeinventory.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.ojt.ecommerce.enums.BrandStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandResponseDto {

    private Long brandId;

    private String brandName;

    private String brandLogoUrl;

    private String description;

    private BrandStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;

    private List<Long> categoryIds;
}

