package com.ojt.ecommerce.storefront.refund.controller;

import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import com.ojt.ecommerce.storefront.refund.dto.*;
import com.ojt.ecommerce.storefront.refund.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storefront/v1/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundResponseDto> createRefundRequest(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestBody RefundRequestDto request) {
        return ResponseEntity.ok(refundService.createRefundRequest(principal.getCustomerId(), request));
    }

    @GetMapping
    public ResponseEntity<List<RefundResponseDto>> getMyRefunds(
            @AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(refundService.getMyRefunds(principal.getCustomerId()));
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponseDto> getRefundById(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long refundId) {
        return ResponseEntity.ok(refundService.getRefundById(principal.getCustomerId(), refundId));
    }

    @GetMapping("/eligibility/{orderItemId}")
    public ResponseEntity<RefundEligibilityDto> getRefundEligibility(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long orderItemId) {
        return ResponseEntity.ok(refundService.getRefundEligibility(principal.getCustomerId(), orderItemId));
    }
}
