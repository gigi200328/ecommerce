package com.ojt.ecommerce.storefront.searchhistory.repository;

import com.ojt.ecommerce.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    // 1. Existing customer + keyword lookup
    Optional<SearchHistory> findByCustomerCustomerIdAndKeyword(Long customerId, String keyword);

    // 2. Recent searches
    List<SearchHistory> findTop10ByCustomerCustomerIdOrderBySearchedAtDesc(Long customerId);

    @Modifying
    @Transactional
    long deleteByCustomerCustomerIdAndKeyword(Long customerId, String keyword);

    @Modifying
    @Transactional
    long deleteByCustomerCustomerId(Long customerId);

    // 3. Atomic recent-search update
    @Modifying
    @Transactional
    @Query("UPDATE SearchHistory s SET s.searchedAt = :searchedAt WHERE s.customer.customerId = :customerId AND s.keyword = :keyword")
    int updateSearchedAt(@Param("customerId") Long customerId, @Param("keyword") String keyword, @Param("searchedAt") LocalDateTime searchedAt);

    // POPULAR SP MAPPING:
    @Query(value = "CALL sp_get_popular_searches()", nativeQuery = true)
    List<com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchProjection> getPopularSearches();
}
