package com.ojt.ecommerce.storefront.searchhistory.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PopularSearchResponse {
    
    private String keyword;
    private Long totalCount;
    
}
