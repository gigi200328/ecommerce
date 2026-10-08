package com.ojt.ecommerce.storefront.searchhistory.repository;

import com.ojt.ecommerce.entity.SearchKeywordStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface SearchKeywordStatsRepository extends JpaRepository<SearchKeywordStats, String> {

    @Modifying
    @Transactional
    @Query("UPDATE SearchKeywordStats s SET s.totalSearchCount = s.totalSearchCount + 1, s.lastSearchedAt = :searchedAt WHERE s.keyword = :keyword")
    int incrementSearchCount(@Param("keyword") String keyword, @Param("searchedAt") LocalDateTime searchedAt);

    java.util.List<SearchKeywordStats> findByKeywordStartingWith(String prefix);
}
