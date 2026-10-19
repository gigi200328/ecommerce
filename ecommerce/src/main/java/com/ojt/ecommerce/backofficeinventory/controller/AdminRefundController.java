package com.ojt.ecommerce.backofficeinventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.AdminRefundResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.RefundDecisionDto;
import com.ojt.ecommerce.backofficeinventory.dto.RefundStatsDto;
import com.ojt.ecommerce.backofficeinventory.service.AdminRefundService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/admin/refunds", "/api/backoffice/refunds"})
@RequiredArgsConstructor
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    /**
     * Fetch refund records directly from the refund table joined with orders and customers.
     * Supports optional query parameters: search, status (ALL, PENDING, APPROVED, REJECTED), startDate, endDate.
     */
    @GetMapping
    public ResponseEntity<List<AdminRefundResponseDto>> getRefunds(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<AdminRefundResponseDto> refunds = adminRefundService.getRefunds(search, status, startDate, endDate);
        return ResponseEntity.ok(refunds);
    }

    /**
     * Return summary statistics (totalRequests, pendingCount, approvedCount, rejectedCount, totalRefundedAmount).
     */
    @GetMapping("/stats")
    public ResponseEntity<RefundStatsDto> getRefundStats() {
        RefundStatsDto stats = adminRefundService.getRefundStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Approve a pending refund request and update the linked order's payment_status to 'REFUNDED'.
     */
    @PutMapping("/{refundId}/approve")
    public ResponseEntity<AdminRefundResponseDto> approveRefund(
            @PathVariable Long refundId,
            @RequestBody(required = false) RefundDecisionDto request,
            @RequestParam(value = "adminNote", required = false) String adminNote) {
        if (request == null) {
            request = new RefundDecisionDto();
        }
        if ((request.getAdminNote() == null || request.getAdminNote().isBlank()) && adminNote != null) {
            request.setAdminNote(adminNote);
        }
        AdminRefundResponseDto response = adminRefundService.approveRefund(refundId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a pending refund request with a required admin rejection note.
     */
    @PutMapping("/{refundId}/reject")
    public ResponseEntity<AdminRefundResponseDto> rejectRefund(
            @PathVariable Long refundId,
            @RequestBody(required = false) RefundDecisionDto request,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "adminNote", required = false) String adminNote) {
        if (request == null) {
            request = new RefundDecisionDto();
        }
        if (request.getAdminNote() == null || request.getAdminNote().isBlank()) {
            if (adminNote != null && !adminNote.isBlank()) {
                request.setAdminNote(adminNote);
            } else if (reason != null && !reason.isBlank()) {
                request.setAdminNote(reason);
            }
        }
        AdminRefundResponseDto response = adminRefundService.rejectRefund(refundId, request);
        return ResponseEntity.ok(response);
    }
}
