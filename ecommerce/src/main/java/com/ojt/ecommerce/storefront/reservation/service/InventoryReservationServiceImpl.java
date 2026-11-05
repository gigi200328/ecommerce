package com.ojt.ecommerce.storefront.reservation.service;

import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.storefront.catalog.repository.G5InventoryRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.reservation.dto.ReservationItem;
import com.ojt.ecommerce.storefront.reservation.entity.InventoryReservation;
import com.ojt.ecommerce.storefront.reservation.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReservationServiceImpl implements InventoryReservationService {

    private final InventoryReservationRepository reservationRepository;
    private final G5InventoryRepository inventoryRepository;
    private final G5OrderItemRepository orderItemRepository;

    @Value("${storefront.inventory.reservation-ttl-minutes:3}")
    private int reservationTtlMinutes = 3;

    @Override
    @Transactional
    public void reserveStockForOrder(Order order, List<ReservationItem> items) {
        if (order == null || items == null || items.isEmpty()) {
            return;
        }

        // Sort items by variantId ascending to enforce deterministic lock ordering and prevent deadlocks
        List<ReservationItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(item -> item.getVariant().getVariantId()));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(reservationTtlMinutes);

        for (ReservationItem item : sortedItems) {
            Long variantId = item.getVariant().getVariantId();
            int requestedQty = item.getQuantity();

            if (requestedQty <= 0) {
                throw new InvalidRequestException("Reservation quantity must be greater than zero");
            }

            // Lock inventory row with PESSIMISTIC_WRITE (SELECT FOR UPDATE)
            Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId)
                    .orElseThrow(() -> new InvalidRequestException("Inventory record not found for variant ID: " + variantId));

            int physical = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int activeReserved = reservationRepository.sumActiveReservedQuantityByVariant(variantId, now);
            int available = physical - activeReserved;

            if (available < requestedQty) {
                String sku = item.getVariant().getSku() != null ? item.getVariant().getSku() : String.valueOf(variantId);
                log.warn("Insufficient stock to reserve SKU {} for order #{}. Physical: {}, Reserved: {}, Available: {}, Requested: {}",
                        sku, order.getOrderId(), physical, activeReserved, available, requestedQty);
                throw new InvalidRequestException("Insufficient stock for product: " + sku +
                        ". Available: " + Math.max(0, available) + ", Requested: " + requestedQty);
            }

            // Check if reservation already exists for this order + variant (idempotency)
            Optional<InventoryReservation> existingOpt = reservationRepository.findByOrderOrderIdAndVariantVariantId(
                    order.getOrderId(), variantId);

            InventoryReservation reservation;
            if (existingOpt.isPresent()) {
                reservation = existingOpt.get();
                reservation.setQuantity(requestedQty);
                reservation.setStatus(InventoryReservation.STATUS_RESERVED);
                reservation.setExpiresAt(expiresAt);
                reservation.setModifiedAt(now);
            } else {
                reservation = InventoryReservation.builder()
                        .order(order)
                        .variant(item.getVariant())
                        .quantity(requestedQty)
                        .status(InventoryReservation.STATUS_RESERVED)
                        .expiresAt(expiresAt)
                        .createdAt(now)
                        .build();
            }
            reservationRepository.save(reservation);

            // Synchronize inventory.reserved_quantity summary field
            int newTotalReserved = activeReserved + requestedQty;
            inv.setReservedQuantity(newTotalReserved);
            inv.setModifiedAt(now);
            inventoryRepository.save(inv);

            log.info("Reserved {} units of variant #{} for order #{}. Physical: {}, New Reserved: {}, Available: {}",
                    requestedQty, variantId, order.getOrderId(), physical, newTotalReserved, physical - newTotalReserved);
        }
    }

    @Override
    @Transactional
    public void consumeReservationsForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }

        List<InventoryReservation> reservations = reservationRepository.findByOrderOrderIdAndStatus(
                orderId, InventoryReservation.STATUS_RESERVED);

        if (reservations.isEmpty()) {
            log.info("No active reservations found to consume for order #{}", orderId);
            return;
        }

        // Sort reservations by variantId ascending for lock ordering
        reservations.sort(Comparator.comparing(r -> r.getVariant().getVariantId()));
        LocalDateTime now = LocalDateTime.now();

        for (InventoryReservation res : reservations) {
            res.setStatus(InventoryReservation.STATUS_CONSUMED);
            res.setModifiedAt(now);
            reservationRepository.save(res);

            Long variantId = res.getVariant().getVariantId();
            Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId).orElse(null);
            if (inv != null) {
                int remainingReserved = reservationRepository.sumActiveReservedQuantityByVariant(variantId, now);
                inv.setReservedQuantity(remainingReserved);
                inv.setModifiedAt(now);
                inventoryRepository.save(inv);
                log.info("Consumed reservation #{} (qty: {}) for order #{}. Remaining reserved for variant #{}: {}",
                        res.getReservationId(), res.getQuantity(), orderId, variantId, remainingReserved);
            }
        }
    }

    @Override
    @Transactional
    public void releaseReservationsForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }

        List<InventoryReservation> reservations = reservationRepository.findByOrderOrderIdAndStatus(
                orderId, InventoryReservation.STATUS_RESERVED);

        if (reservations.isEmpty()) {
            log.info("No active reservations found to release for order #{}", orderId);
            return;
        }

        reservations.sort(Comparator.comparing(r -> r.getVariant().getVariantId()));
        LocalDateTime now = LocalDateTime.now();

        for (InventoryReservation res : reservations) {
            res.setStatus(InventoryReservation.STATUS_RELEASED);
            res.setModifiedAt(now);
            reservationRepository.save(res);

            Long variantId = res.getVariant().getVariantId();
            Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId).orElse(null);
            if (inv != null) {
                int remainingReserved = reservationRepository.sumActiveReservedQuantityByVariant(variantId, now);
                inv.setReservedQuantity(remainingReserved);
                inv.setModifiedAt(now);
                inventoryRepository.save(inv);
                log.info("Released reservation #{} (qty: {}) for order #{}. Remaining reserved for variant #{}: {}",
                        res.getReservationId(), res.getQuantity(), orderId, variantId, remainingReserved);
            }
        }
    }

    @Override
    @Transactional
    public List<Long> expireAbandonedReservationsAndGetOrderIds() {
        LocalDateTime now = LocalDateTime.now();
        List<InventoryReservation> expired = reservationRepository.findExpiredReservations(now);

        if (expired.isEmpty()) {
            return Collections.emptyList();
        }

        // Group expired reservations by variantId to re-sync summary once per variant
        Set<Long> affectedVariantIds = new HashSet<>();
        Set<Long> affectedOrderIds = new LinkedHashSet<>();
        for (InventoryReservation res : expired) {
            res.setStatus(InventoryReservation.STATUS_EXPIRED);
            res.setModifiedAt(now);
            reservationRepository.save(res);
            if (res.getVariant() != null) {
                affectedVariantIds.add(res.getVariant().getVariantId());
            }
            if (res.getOrder() != null && res.getOrder().getOrderId() != null) {
                affectedOrderIds.add(res.getOrder().getOrderId());
            }
        }

        // Re-sync inventory.reserved_quantity for each affected variant
        List<Long> sortedVariantIds = new ArrayList<>(affectedVariantIds);
        Collections.sort(sortedVariantIds);

        for (Long variantId : sortedVariantIds) {
            Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId).orElse(null);
            if (inv != null) {
                int activeReserved = reservationRepository.sumActiveReservedQuantityByVariant(variantId, now);
                inv.setReservedQuantity(activeReserved);
                inv.setModifiedAt(now);
                inventoryRepository.save(inv);
            }
        }

        log.info("Expired {} abandoned inventory reservations across {} variants and {} orders.",
                expired.size(), affectedVariantIds.size(), affectedOrderIds.size());
        return new ArrayList<>(affectedOrderIds);
    }

    @Override
    @Transactional
    public int expireAbandonedReservations() {
        return expireAbandonedReservationsAndGetOrderIds().size();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableStock(Long variantId) {
        if (variantId == null) {
            return 0;
        }
        Inventory inv = inventoryRepository.findByVariantVariantId(variantId).orElse(null);
        if (inv == null) {
            return 0;
        }
        int physical = inv.getQuantity() != null ? inv.getQuantity() : 0;
        int activeReserved = reservationRepository.sumActiveReservedQuantityByVariant(variantId, LocalDateTime.now());
        return Math.max(0, physical - activeReserved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> getAvailableStockForVariants(List<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Inventory> inventories = inventoryRepository.findByVariantVariantIdIn(variantIds);
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> activeReservedRows = reservationRepository.sumActiveReservedQuantityByVariantIds(variantIds, now);

        Map<Long, Integer> reservedMap = new HashMap<>();
        for (Object[] row : activeReservedRows) {
            Long vId = (Long) row[0];
            int sum = ((Number) row[1]).intValue();
            reservedMap.put(vId, sum);
        }

        Map<Long, Integer> resultMap = new HashMap<>();
        for (Inventory inv : inventories) {
            if (inv.getVariant() != null) {
                Long vId = inv.getVariant().getVariantId();
                int physical = inv.getQuantity() != null ? inv.getQuantity() : 0;
                int reserved = reservedMap.getOrDefault(vId, 0);
                resultMap.put(vId, Math.max(0, physical - reserved));
            }
        }

        return resultMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> getReservationExpiryForOrder(Long orderId) {
        if (orderId == null) {
            return Optional.empty();
        }
        List<InventoryReservation> reservations = reservationRepository.findByOrderOrderId(orderId);
        if (reservations.isEmpty()) {
            return Optional.empty();
        }
        return reservations.stream()
                .map(InventoryReservation::getExpiresAt)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isReservationActiveForOrder(Long orderId) {
        if (orderId == null) {
            return false;
        }
        List<InventoryReservation> reservations = reservationRepository.findByOrderOrderIdAndStatus(
                orderId, InventoryReservation.STATUS_RESERVED);
        if (reservations.isEmpty()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return reservations.stream()
                .anyMatch(r -> r.getExpiresAt() != null && r.getExpiresAt().isAfter(now));
    }

    @Override
    @Transactional
    public void reReserveStockForOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new InvalidRequestException("Cannot re-reserve stock: order is null");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderOrderId(order.getOrderId());
        if (orderItems == null || orderItems.isEmpty()) {
            throw new InvalidRequestException("Cannot re-reserve stock: order has no items");
        }

        // Map to ReservationItem and sort by variantId ascending for lock ordering
        List<ReservationItem> items = orderItems.stream()
                .map(oi -> new ReservationItem(oi.getVariant(), oi.getQty()))
                .sorted(Comparator.comparing(item -> item.getVariant().getVariantId()))
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(reservationTtlMinutes);

        for (ReservationItem item : items) {
            Long variantId = item.getVariant().getVariantId();
            int requestedQty = item.getQuantity();

            if (requestedQty <= 0) {
                throw new InvalidRequestException("Reservation quantity must be greater than zero");
            }

            // Lock inventory row with PESSIMISTIC_WRITE (SELECT FOR UPDATE)
            Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId)
                    .orElseThrow(() -> new InvalidRequestException("Inventory record not found for variant ID: " + variantId));

            int physical = inv.getQuantity() != null ? inv.getQuantity() : 0;
            int activeReserved = reservationRepository.sumActiveReservedQuantityByVariant(variantId, now);

            // Check if reservation already exists for this order + variant
            Optional<InventoryReservation> existingOpt = reservationRepository.findByOrderOrderIdAndVariantVariantId(
                    order.getOrderId(), variantId);

            int thisOrderActiveQty = 0;
            if (existingOpt.isPresent()) {
                InventoryReservation existingRes = existingOpt.get();
                if (InventoryReservation.STATUS_RESERVED.equals(existingRes.getStatus())
                        && existingRes.getExpiresAt() != null
                        && existingRes.getExpiresAt().isAfter(now)) {
                    thisOrderActiveQty = existingRes.getQuantity() != null ? existingRes.getQuantity() : 0;
                }
            }

            int available = physical - (activeReserved - thisOrderActiveQty);

            if (available < requestedQty) {
                String sku = item.getVariant().getSku() != null ? item.getVariant().getSku() : String.valueOf(variantId);
                log.warn("Insufficient stock to re-reserve SKU {} for order #{}. Physical: {}, Reserved: {}, Available: {}, Requested: {}",
                        sku, order.getOrderId(), physical, activeReserved, available, requestedQty);
                throw new InvalidRequestException("Insufficient stock for product: " + sku +
                        ". Available: " + Math.max(0, available) + ", Requested: " + requestedQty);
            }

            InventoryReservation reservation;
            if (existingOpt.isPresent()) {
                reservation = existingOpt.get();
                reservation.setQuantity(requestedQty);
                reservation.setStatus(InventoryReservation.STATUS_RESERVED);
                reservation.setExpiresAt(expiresAt);
                reservation.setModifiedAt(now);
            } else {
                reservation = InventoryReservation.builder()
                        .order(order)
                        .variant(item.getVariant())
                        .quantity(requestedQty)
                        .status(InventoryReservation.STATUS_RESERVED)
                        .expiresAt(expiresAt)
                        .createdAt(now)
                        .build();
            }
            reservationRepository.save(reservation);

            // Re-sync inventory.reserved_quantity summary field
            int newTotalReserved = (activeReserved - thisOrderActiveQty) + requestedQty;
            inv.setReservedQuantity(newTotalReserved);
            inv.setModifiedAt(now);
            inventoryRepository.save(inv);

            log.info("Re-reserved {} units of variant #{} for order #{}. Physical: {}, New Reserved: {}, Available: {}",
                    requestedQty, variantId, order.getOrderId(), physical, newTotalReserved, physical - newTotalReserved);
        }
    }
}
