package com.ojt.ecommerce.storefront;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.SearchHistory;
import com.ojt.ecommerce.entity.SearchKeywordStats;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchProjection;
import com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchResponse;
import com.ojt.ecommerce.storefront.searchhistory.repository.SearchHistoryRepository;
import com.ojt.ecommerce.storefront.searchhistory.repository.SearchKeywordStatsRepository;
import com.ojt.ecommerce.storefront.searchhistory.service.SearchHistoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceImplTest {

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private SearchKeywordStatsRepository searchKeywordStatsRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private SearchHistoryServiceImpl searchHistoryService;

    private Customer mockCustomer;

    @BeforeEach
    void setUp() {
        mockCustomer = Customer.builder()
                .customerId(1L)
                .fullName("Test Customer")
                .email("customer@test.com")
                .status("ACTIVE")
                .build();
    }

    // =========================================================================
    // P0 TESTS: Critical Business Logic
    // =========================================================================

    @Test
    @DisplayName("P0 - recordSearch: new keyword creates personal history and global stats records")
    void recordSearch_newKeyword_createsPersonalAndGlobalRecords() {
        // Arrange
        Long customerId = 1L;
        String keyword = "hoodie";

        when(searchHistoryRepository.updateSearchedAt(eq(customerId), eq("hoodie"), any(LocalDateTime.class)))
                .thenReturn(0);
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(mockCustomer));
        when(searchKeywordStatsRepository.incrementSearchCount(eq("hoodie"), any(LocalDateTime.class)))
                .thenReturn(0);

        // Act
        searchHistoryService.recordSearch(customerId, keyword);

        // Assert & Verify
        verify(searchHistoryRepository).updateSearchedAt(eq(customerId), eq("hoodie"), any(LocalDateTime.class));
        verify(customerRepository).findById(customerId);
        verify(searchKeywordStatsRepository).incrementSearchCount(eq("hoodie"), any(LocalDateTime.class));

        ArgumentCaptor<SearchHistory> historyCaptor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(searchHistoryRepository).save(historyCaptor.capture());
        SearchHistory capturedHistory = historyCaptor.getValue();
        assertEquals("hoodie", capturedHistory.getKeyword());
        assertEquals(mockCustomer, capturedHistory.getCustomer());
        assertNotNull(capturedHistory.getSearchedAt());

        ArgumentCaptor<SearchKeywordStats> statsCaptor = ArgumentCaptor.forClass(SearchKeywordStats.class);
        verify(searchKeywordStatsRepository).save(statsCaptor.capture());
        SearchKeywordStats capturedStats = statsCaptor.getValue();
        assertEquals("hoodie", capturedStats.getKeyword());
        assertEquals(1L, capturedStats.getTotalSearchCount());
        assertNotNull(capturedStats.getLastSearchedAt());
    }

    @Test
    @DisplayName("P0 - recordSearch: existing keyword updates timestamp/count without inserting new rows")
    void recordSearch_existingKeyword_updatesWithoutInsert() {
        // Arrange
        Long customerId = 1L;
        String keyword = "shirt";

        when(searchHistoryRepository.updateSearchedAt(eq(customerId), eq("shirt"), any(LocalDateTime.class)))
                .thenReturn(1);
        when(searchKeywordStatsRepository.incrementSearchCount(eq("shirt"), any(LocalDateTime.class)))
                .thenReturn(1);

        // Act
        searchHistoryService.recordSearch(customerId, keyword);

        // Assert & Verify
        verify(searchHistoryRepository).updateSearchedAt(eq(customerId), eq("shirt"), any(LocalDateTime.class));
        verify(searchKeywordStatsRepository).incrementSearchCount(eq("shirt"), any(LocalDateTime.class));

        verify(customerRepository, never()).findById(any());
        verify(searchHistoryRepository, never()).save(any());
        verify(searchKeywordStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("P0 - recordSearch: trims whitespace and lowercases keyword before repository calls")
    void recordSearch_trimsAndLowercasesKeyword() {
        // Arrange
        Long customerId = 1L;
        String rawKeyword = "  T-SHIRT  ";

        when(searchHistoryRepository.updateSearchedAt(eq(customerId), eq("t-shirt"), any(LocalDateTime.class)))
                .thenReturn(1);
        when(searchKeywordStatsRepository.incrementSearchCount(eq("t-shirt"), any(LocalDateTime.class)))
                .thenReturn(1);

        // Act
        searchHistoryService.recordSearch(customerId, rawKeyword);

        // Assert & Verify
        verify(searchHistoryRepository).updateSearchedAt(eq(customerId), eq("t-shirt"), any(LocalDateTime.class));
        verify(searchKeywordStatsRepository).incrementSearchCount(eq("t-shirt"), any(LocalDateTime.class));
        verify(searchHistoryRepository, never()).updateSearchedAt(eq(customerId), eq(rawKeyword), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("P0 - deleteSearch: deletes personal history and strictly avoids touching global stats")
    void deleteSearch_doesNotTouchGlobalStats() {
        // Arrange
        Long customerId = 1L;
        String keyword = "jeans";

        // Act
        searchHistoryService.deleteSearch(customerId, keyword);

        // Assert & Verify
        verify(searchHistoryRepository).deleteByCustomerCustomerIdAndKeyword(customerId, "jeans");
        verifyNoInteractions(searchKeywordStatsRepository);
    }

    @Test
    @DisplayName("P0 - clearSearchHistory: deletes all personal history and strictly avoids touching global stats")
    void clearSearchHistory_doesNotTouchGlobalStats() {
        // Arrange
        Long customerId = 1L;

        // Act
        searchHistoryService.clearSearchHistory(customerId);

        // Assert & Verify
        verify(searchHistoryRepository).deleteByCustomerCustomerId(customerId);
        verifyNoInteractions(searchKeywordStatsRepository);
    }

    // =========================================================================
    // P1 TESTS: Important Edge Cases & Business Constraints
    // =========================================================================

    @Test
    @DisplayName("P1 - recordSearch: blank/whitespace keyword throws InvalidRequestException")
    void recordSearch_blankKeyword_throws() {
        // Arrange
        Long customerId = 1L;
        String blankKeyword = "   ";

        // Act & Assert
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> searchHistoryService.recordSearch(customerId, blankKeyword)
        );

        assertEquals("Search keyword cannot be empty", exception.getMessage());
        verifyNoInteractions(searchHistoryRepository, searchKeywordStatsRepository, customerRepository);
    }

    @Test
    @DisplayName("P1 - recordSearch: null keyword throws InvalidRequestException")
    void recordSearch_nullKeyword_throws() {
        // Arrange
        Long customerId = 1L;

        // Act & Assert
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> searchHistoryService.recordSearch(customerId, null)
        );

        assertEquals("Search keyword cannot be empty", exception.getMessage());
        verifyNoInteractions(searchHistoryRepository, searchKeywordStatsRepository, customerRepository);
    }

    @Test
    @DisplayName("P1 - recordSearch: non-existent customer throws ResourceNotFoundException without global save")
    void recordSearch_customerNotFound_throws() {
        // Arrange
        Long invalidCustomerId = 999L;
        String keyword = "pants";

        when(searchHistoryRepository.updateSearchedAt(eq(invalidCustomerId), eq("pants"), any(LocalDateTime.class)))
                .thenReturn(0);
        when(customerRepository.findById(invalidCustomerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> searchHistoryService.recordSearch(invalidCustomerId, keyword)
        );

        assertEquals("Customer not found", exception.getMessage());
        verify(searchHistoryRepository, never()).save(any());
        verify(searchKeywordStatsRepository, never()).incrementSearchCount(any(), any());
        verify(searchKeywordStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("P1 - getRecentSearches: returns mapped recent search entities in repository order")
    void getRecentSearches_mapsResults() {
        // Arrange
        Long customerId = 1L;
        LocalDateTime now = LocalDateTime.now();

        SearchHistory h1 = SearchHistory.builder().searchId(1L).keyword("shirt").searchedAt(now).build();
        SearchHistory h2 = SearchHistory.builder().searchId(2L).keyword("shoes").searchedAt(now.minusMinutes(5)).build();
        SearchHistory h3 = SearchHistory.builder().searchId(3L).keyword("hat").searchedAt(now.minusHours(1)).build();
        List<SearchHistory> mockHistories = List.of(h1, h2, h3);

        when(searchHistoryRepository.findTop10ByCustomerCustomerIdOrderBySearchedAtDesc(customerId))
                .thenReturn(mockHistories);

        // Act
        List<SearchHistory> result = searchHistoryService.getRecentSearches(customerId);

        // Assert & Verify
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("shirt", result.get(0).getKeyword());
        assertEquals("shoes", result.get(1).getKeyword());
        assertEquals("hat", result.get(2).getKeyword());
        verify(searchHistoryRepository).findTop10ByCustomerCustomerIdOrderBySearchedAtDesc(customerId);
    }

    @Test
    @DisplayName("P1 - deleteSearch: normalizes trimmed/lowercased keyword before deleting")
    void deleteSearch_normalizesKeyword() {
        // Arrange
        Long customerId = 1L;
        String rawKeyword = "  SHOES ";

        // Act
        searchHistoryService.deleteSearch(customerId, rawKeyword);

        // Assert & Verify
        verify(searchHistoryRepository).deleteByCustomerCustomerIdAndKeyword(customerId, "shoes");
        verify(searchHistoryRepository, never()).deleteByCustomerCustomerIdAndKeyword(customerId, rawKeyword);
    }

    // =========================================================================
    // P2 TESTS: Secondary & Projection Mapping Tests
    // =========================================================================

    @Test
    @DisplayName("P2 - getRecentSearches: returns empty list when customer has no searches")
    void getRecentSearches_empty_returnsEmptyList() {
        // Arrange
        Long customerId = 1L;
        when(searchHistoryRepository.findTop10ByCustomerCustomerIdOrderBySearchedAtDesc(customerId))
                .thenReturn(Collections.emptyList());

        // Act
        List<SearchHistory> result = searchHistoryService.getRecentSearches(customerId);

        // Assert & Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(searchHistoryRepository).findTop10ByCustomerCustomerIdOrderBySearchedAtDesc(customerId);
    }

    @Test
    @DisplayName("P2 - getPopularSearches: maps repository projection list to PopularSearchResponse DTOs")
    void getPopularSearches_mapsProjection() {
        // Arrange
        PopularSearchProjection proj1 = mock(PopularSearchProjection.class);
        when(proj1.getKeyword()).thenReturn("shirt");
        when(proj1.getTotalCount()).thenReturn(10L);

        PopularSearchProjection proj2 = mock(PopularSearchProjection.class);
        when(proj2.getKeyword()).thenReturn("jeans");
        when(proj2.getTotalCount()).thenReturn(5L);

        when(searchHistoryRepository.getPopularSearches())
                .thenReturn(List.of(proj1, proj2));

        // Act
        List<PopularSearchResponse> result = searchHistoryService.getPopularSearches();

        // Assert & Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("shirt", result.get(0).getKeyword());
        assertEquals(10L, result.get(0).getTotalCount());
        assertEquals("jeans", result.get(1).getKeyword());
        assertEquals(5L, result.get(1).getTotalCount());
        verify(searchHistoryRepository).getPopularSearches();
    }
}
