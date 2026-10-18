package com.ojt.ecommerce.storefront.searchhistory.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.SearchHistory;
import com.ojt.ecommerce.entity.SearchKeywordStats;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.searchhistory.repository.SearchHistoryRepository;
import com.ojt.ecommerce.storefront.searchhistory.repository.SearchKeywordStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final SearchKeywordStatsRepository searchKeywordStatsRepository;
    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void recordSearch(Long customerId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new InvalidRequestException("Search keyword cannot be empty");
        }
        
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();

        // 1. Update PERSONAL history
        int personalUpdated = searchHistoryRepository.updateSearchedAt(customerId, normalizedKeyword, now);
        if (personalUpdated == 0) {
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

            SearchHistory newSearch = SearchHistory.builder()
                    .customer(customer)
                    .keyword(normalizedKeyword)
                    .searchedAt(now)
                    .build();
            searchHistoryRepository.save(newSearch);
        }

        // 2. Update GLOBAL stats
        int globalUpdated = searchKeywordStatsRepository.incrementSearchCount(normalizedKeyword, now);
        if (globalUpdated == 0) {
            SearchKeywordStats newStat = SearchKeywordStats.builder()
                    .keyword(normalizedKeyword)
                    .totalSearchCount(1L)
                    .lastSearchedAt(now)
                    .build();
            searchKeywordStatsRepository.save(newStat);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistory> getRecentSearches(Long customerId) {
        return searchHistoryRepository.findTop10ByCustomerCustomerIdOrderBySearchedAtDesc(customerId);
    }

    @Override
    @Transactional
    public void deleteSearch(Long customerId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new InvalidRequestException("Search keyword cannot be empty");
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        searchHistoryRepository.deleteByCustomerCustomerIdAndKeyword(customerId, normalizedKeyword);
    }

    @Override
    @Transactional
    public void clearSearchHistory(Long customerId) {
        searchHistoryRepository.deleteByCustomerCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchResponse> getPopularSearches() {
        return searchHistoryRepository.getPopularSearches().stream()
                .map(projection -> com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchResponse.builder()
                        .keyword(projection.getKeyword())
                        .totalCount(projection.getTotalCount())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
}
