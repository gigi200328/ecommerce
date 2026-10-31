package com.ojt.ecommerce.storefront.searchhistory.controller;

import com.ojt.ecommerce.entity.SearchHistory;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.searchhistory.dto.PopularSearchResponse;
import com.ojt.ecommerce.storefront.searchhistory.dto.RecentSearchResponse;
import com.ojt.ecommerce.storefront.searchhistory.dto.SearchHistoryRequest;
import com.ojt.ecommerce.storefront.searchhistory.service.SearchHistoryService;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryControllerTest {

    @Mock
    private SearchHistoryService searchHistoryService;

    @InjectMocks
    private SearchHistoryController searchHistoryController;

    private CustomerPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");
    }

    @Test
    @DisplayName("1. recordSearch - returns 200 OK when recording a valid search keyword")
    void recordSearch_returns200Ok() {
        SearchHistoryRequest request = new SearchHistoryRequest();
        request.setKeyword("laptop");

        doNothing().when(searchHistoryService).recordSearch(1L, "laptop");

        ResponseEntity<Void> response = searchHistoryController.recordSearch(principal, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(searchHistoryService, times(1)).recordSearch(1L, "laptop");
    }

    @Test
    @DisplayName("2. recordSearch - propagates InvalidRequestException when service validation fails")
    void recordSearch_serviceExceptionPropagation() {
        SearchHistoryRequest request = new SearchHistoryRequest();
        request.setKeyword(" ");

        doThrow(new InvalidRequestException("Search keyword cannot be empty"))
                .when(searchHistoryService).recordSearch(1L, " ");

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> searchHistoryController.recordSearch(principal, request));

        assertEquals("Search keyword cannot be empty", ex.getMessage());
        verify(searchHistoryService, times(1)).recordSearch(1L, " ");
    }

    @Test
    @DisplayName("3. getRecentSearches - returns 200 OK with mapped RecentSearchResponse list when populated")
    void getRecentSearches_populatedResponse() {
        LocalDateTime now = LocalDateTime.now();
        List<SearchHistory> mockHistories = List.of(
                SearchHistory.builder().searchId(1L).keyword("shoes").searchedAt(now).build(),
                SearchHistory.builder().searchId(2L).keyword("jacket").searchedAt(now.minusHours(1)).build()
        );

        when(searchHistoryService.getRecentSearches(1L)).thenReturn(mockHistories);

        ResponseEntity<List<RecentSearchResponse>> response = searchHistoryController.getRecentSearches(principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("shoes", response.getBody().get(0).getKeyword());
        assertEquals(now, response.getBody().get(0).getSearchedAt());
        assertEquals("jacket", response.getBody().get(1).getKeyword());
        verify(searchHistoryService, times(1)).getRecentSearches(1L);
    }

    @Test
    @DisplayName("4. getRecentSearches - returns 200 OK with empty list when no recent searches exist")
    void getRecentSearches_emptyResponse() {
        when(searchHistoryService.getRecentSearches(1L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<RecentSearchResponse>> response = searchHistoryController.getRecentSearches(principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(searchHistoryService, times(1)).getRecentSearches(1L);
    }

    @Test
    @DisplayName("5. getPopularSearches - returns 200 OK with popular search responses")
    void getPopularSearches_populatedResponse() {
        List<PopularSearchResponse> mockPopular = List.of(
                PopularSearchResponse.builder().keyword("hoodie").totalCount(50L).build(),
                PopularSearchResponse.builder().keyword("t-shirt").totalCount(30L).build()
        );

        when(searchHistoryService.getPopularSearches()).thenReturn(mockPopular);

        ResponseEntity<List<PopularSearchResponse>> response = searchHistoryController.getPopularSearches();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("hoodie", response.getBody().get(0).getKeyword());
        assertEquals(50L, response.getBody().get(0).getTotalCount());
        verify(searchHistoryService, times(1)).getPopularSearches();
    }

    @Test
    @DisplayName("6. deleteSearch - returns 200 OK when deleting a search keyword")
    void deleteSearch_returns200Ok() {
        String keyword = "shoes";

        doNothing().when(searchHistoryService).deleteSearch(1L, keyword);

        ResponseEntity<Void> response = searchHistoryController.deleteSearch(principal, keyword);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(searchHistoryService, times(1)).deleteSearch(1L, keyword);
    }

    @Test
    @DisplayName("7. clearAll - returns 200 OK when clearing all search history")
    void clearAll_returns200Ok() {
        doNothing().when(searchHistoryService).clearSearchHistory(1L);

        ResponseEntity<Void> response = searchHistoryController.clearAll(principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(searchHistoryService, times(1)).clearSearchHistory(1L);
    }
}
