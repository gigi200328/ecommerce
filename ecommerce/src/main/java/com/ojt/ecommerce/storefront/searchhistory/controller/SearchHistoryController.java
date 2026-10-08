package com.ojt.ecommerce.storefront.searchhistory.controller;

import com.ojt.ecommerce.entity.SearchHistory;
import com.ojt.ecommerce.storefront.searchhistory.dto.RecentSearchResponse;
import com.ojt.ecommerce.storefront.searchhistory.dto.SearchHistoryRequest;
import com.ojt.ecommerce.storefront.searchhistory.service.SearchHistoryService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/storefront/v1/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @PostMapping
    public ResponseEntity<Void> recordSearch(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @RequestBody SearchHistoryRequest request) {
        
        searchHistoryService.recordSearch(principal.getCustomerId(), request.getKeyword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RecentSearchResponse>> getRecentSearches(
            @AuthenticationPrincipal CustomerPrincipal principal) {
        
        List<SearchHistory> recentSearches = searchHistoryService.getRecentSearches(principal.getCustomerId());
        
        List<RecentSearchResponse> response = recentSearches.stream()
                .map(history -> RecentSearchResponse.builder()
                        .keyword(history.getKeyword())
                        .searchedAt(history.getSearchedAt())
                        .build())
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/popular")
    public ResponseEntity<List<com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchResponse>> getPopularSearches() {
        return ResponseEntity.ok(searchHistoryService.getPopularSearches());
    }

    @DeleteMapping("/item")
    public ResponseEntity<Void> deleteSearch(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestParam String keyword) {
        searchHistoryService.deleteSearch(principal.getCustomerId(), keyword);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> clearAll(
            @AuthenticationPrincipal CustomerPrincipal principal) {
        searchHistoryService.clearSearchHistory(principal.getCustomerId());
        return ResponseEntity.ok().build();
    }
}
