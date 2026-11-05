package com.ojt.ecommerce.storefront.reservation.service;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.storefront.reservation.dto.ReservationItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface InventoryReservationService {

    void reserveStockForOrder(Order order, List<ReservationItem> items);

    void consumeReservationsForOrder(Long orderId);

    void releaseReservationsForOrder(Long orderId);

    int expireAbandonedReservations();

    List<Long> expireAbandonedReservationsAndGetOrderIds();

    int getAvailableStock(Long variantId);

    Map<Long, Integer> getAvailableStockForVariants(List<Long> variantIds);

    Optional<LocalDateTime> getReservationExpiryForOrder(Long orderId);

    boolean isReservationActiveForOrder(Long orderId);

    boolean isOrderExpired(Long orderId);

    void reReserveStockForOrder(Order order);
}
