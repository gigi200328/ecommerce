package com.ojt.ecommerce.storefront.searchhistory.service;

import com.ojt.ecommerce.entity.SearchHistory;

import java.util.List;

public interface SearchHistoryService {
    
    void recordSearch(Long customerId, String keyword);
    
    List<SearchHistory> getRecentSearches(Long customerId);

    void deleteSearch(Long customerId, String keyword);

    void clearSearchHistory(Long customerId);

    // POPULAR SERVICE LOGIC:
    List<com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchResponse> getPopularSearches();
}
