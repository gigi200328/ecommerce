
package com.ojt.ecommerce.storefront.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantImageResponse {

    private Long imageId;
    private String imageUrl;
    private Boolean isPrimary;
}
