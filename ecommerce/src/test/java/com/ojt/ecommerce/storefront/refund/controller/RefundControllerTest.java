package com.ojt.ecommerce.storefront.refund.controller;

import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.refund.dto.RefundEligibilityDto;
import com.ojt.ecommerce.storefront.refund.dto.RefundRequestDto;
import com.ojt.ecommerce.storefront.refund.dto.RefundResponseDto;
import com.ojt.ecommerce.storefront.refund.service.RefundService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

    @Mock
    private RefundService refundService;

    @InjectMocks
    private RefundController refundController;

    private CustomerPrincipal principal;
    private RefundRequestDto requestDto;
    private RefundResponseDto responseDto;
    private RefundEligibilityDto eligibilityDto;

    @BeforeEach
    void setUp() {
        principal = new CustomerPrincipal(1L, "customer@example.com");

        requestDto = new RefundRequestDto();
        requestDto.setOrderItemId(101L);
        requestDto.setRefundQuantity(1);
        requestDto.setRequestReason("DAMAGED_ITEM");

        responseDto = RefundResponseDto.builder()
                .refundId(10L)
                .orderItemId(101L)
                .productName("Wireless Mouse")
                .requestedRefundQuantity(1)
                .refundAmount(new BigDecimal("50000.00"))
                .status("PENDING")
                .build();

        eligibilityDto = RefundEligibilityDto.builder()
                .orderItemId(101L)
                .orderedQuantity(2)
                .alreadyReservedRefundQuantity(0)
                .remainingRefundableQuantity(2)
                .canRequestRefund(true)
                .daysRemaining(5)
                .refundStatus(null)
                .build();
    }

    // =========================================================================
    // 1. createRefundRequest Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with created refund response when request is valid")
    void createRefundRequest_Success() {
        when(refundService.createRefundRequest(1L, requestDto)).thenReturn(responseDto);

        ResponseEntity<RefundResponseDto> response = refundController.createRefundRequest(principal, requestDto);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getRefundId());
        assertEquals("PENDING", response.getBody().getStatus());

        verify(refundService, times(1)).createRefundRequest(1L, requestDto);
    }

    @Test
    @DisplayName("Should propagate InvalidRequestException when refund validation fails")
    void createRefundRequest_ThrowsInvalidRequestException() {
        when(refundService.createRefundRequest(1L, requestDto))
                .thenThrow(new InvalidRequestException("Refund can only be requested after the order has been delivered."));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> refundController.createRefundRequest(principal, requestDto));

        assertTrue(ex.getMessage().contains("delivered"));
        verify(refundService, times(1)).createRefundRequest(1L, requestDto);
    }

    // =========================================================================
    // 2. getMyRefunds Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with list of customer refund requests")
    void getMyRefunds_Success() {
        when(refundService.getMyRefunds(1L)).thenReturn(List.of(responseDto));

        ResponseEntity<List<RefundResponseDto>> response = refundController.getMyRefunds(principal);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(10L, response.getBody().get(0).getRefundId());

        verify(refundService, times(1)).getMyRefunds(1L);
    }

    // =========================================================================
    // 3. getRefundById Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with refund detail when refund belongs to customer")
    void getRefundById_Success() {
        when(refundService.getRefundById(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<RefundResponseDto> response = refundController.getRefundById(principal, 10L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().getRefundId());

        verify(refundService, times(1)).getRefundById(1L, 10L);
    }

    @Test
    @DisplayName("Should propagate ResourceNotFoundException when refund request is not found")
    void getRefundById_NotFound() {
        when(refundService.getRefundById(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Refund request not found"));

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> refundController.getRefundById(principal, 999L));

        assertEquals("Refund request not found", ex.getMessage());
        verify(refundService, times(1)).getRefundById(1L, 999L);
    }

    // =========================================================================
    // 4. getRefundEligibility Tests
    // =========================================================================

    @Test
    @DisplayName("Should return 200 OK with refund eligibility information for order item")
    void getRefundEligibility_Success() {
        when(refundService.getRefundEligibility(1L, 101L)).thenReturn(eligibilityDto);

        ResponseEntity<RefundEligibilityDto> response = refundController.getRefundEligibility(principal, 101L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isCanRequestRefund());
        assertEquals(2, response.getBody().getRemainingRefundableQuantity());

        verify(refundService, times(1)).getRefundEligibility(1L, 101L);
    }
}
