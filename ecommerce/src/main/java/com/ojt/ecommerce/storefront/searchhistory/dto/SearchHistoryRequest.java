package com.ojt.ecommerce.storefront.searchhistory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchHistoryRequest {
    
    @NotBlank(message = "Search keyword cannot be blank")
    private String keyword;
    
}
