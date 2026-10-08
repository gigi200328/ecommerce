package com.ojt.ecommerce.storefront.refund.service;
import com.ojt.ecommerce.storefront.refund.dto.*;
import java.util.List;
public interface RefundService {
    RefundResponseDto createRefundRequest(Long customerId, RefundRequestDto dto);
    List<RefundResponseDto> getMyRefunds(Long customerId);
    RefundResponseDto getRefundById(Long customerId, Long refundId);
    RefundEligibilityDto getRefundEligibility(Long customerId, Long orderItemId);
}