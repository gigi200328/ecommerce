package com.ojt.ecommerce.storefront.order.service;
import com.ojt.ecommerce.storefront.order.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
public interface OrderHistoryService {
    Page<OrderListResponse> getMyOrders(Long customerId, String startDate, String endDate, String orderStatus, String orderNo, Pageable pageable);
    OrderDetailResponse getOrderById(Long customerId, Long orderId);
}