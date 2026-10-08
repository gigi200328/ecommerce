package com.ojt.ecommerce.storefront.searchhistory.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RecentSearchResponse {
    
    private String keyword;
    private LocalDateTime searchedAt;
    
}
