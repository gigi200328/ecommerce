package com.ojt.ecommerce.backofficeinventory.dto;

import com.ojt.ecommerce.entity.BrandStatus;
import java.time.LocalDateTime;
import lombok.*;

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
}