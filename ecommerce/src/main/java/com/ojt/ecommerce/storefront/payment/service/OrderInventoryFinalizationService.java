package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.backofficeinventory.repository.InventoryTransactionRepository;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.storefront.catalog.repository.G5InventoryRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInventoryFinalizationService {

    private final G5OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final InventoryTransactionRepository inventoryTxnRepository;
    private final InventoryReservationService reservationService;
    private final G5InventoryRepository inventoryRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    /**
     * Executes inventory stock deduction inside a genuine independent transaction (REQUIRES_NEW).
     * Synchronizes on the Order-level pessimistic lock to serialize concurrent executions from
     * the AFTER_COMMIT listener and the background reconciliation worker.
     *
     * @param orderId the ID of the paid order to finalize
     * @return true if inventory is finalized (or was already finalized), false if order cannot be finalized
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean finalizeInventory(Long orderId) {
        if (orderId == null) {
            return false;
        }

        // 1. Acquire exclusive lock on the Order to serialize listener and reconciler
        Order order = orderRepository.findByIdWithLock(orderId).orElse(null);
        if (order == null) {
            log.warn("Cannot finalize inventory: Order #{} not found.", orderId);
            return false;
        }

        // 2. Validate that order is in PAID / SUCCESS status
        boolean isPaid = "PAID".equalsIgnoreCase(order.getOrderStatus())
                || "SUCCESS".equalsIgnoreCase(order.getPaymentStatus());
        if (!isPaid) {
            log.info("Order #{} is not in PAID/SUCCESS status. Skipping inventory finalization.", orderId);
            return false;
        }

        // Guard against genuinely zero-item orders to avoid endless re-polling
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            log.warn("Cannot finalize inventory: Order #{} has zero items. Skipping.", orderId);
            return false;
        }

        // 3. Atomically recheck idempotency inside the lock
        boolean alreadyDeducted = inventoryTxnRepository
                .existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", orderId);
        if (alreadyDeducted) {
            log.info("Inventory already finalized for order #{}. Skipping.", orderId);
            return true;
        }

        // 4. Validate physical stock availability across all items before deducting.
        // If the reservation expired and physical stock was sold out, prevent overselling.
        List<OrderItem> items = new ArrayList<>(order.getOrderItems());
        items.sort(Comparator.comparing(item -> item.getVariant() != null ? item.getVariant().getVariantId() : 0L));

        boolean hasSufficientStock = true;
        for (OrderItem item : items) {
            if (item.getVariant() == null) {
                continue;
            }
            Long variantId = item.getVariant().getVariantId();
            int deductQty = item.getQty() != null ? item.getQty() : 1;
            Inventory inv = inventoryRepository.findByVariantIdForUpdate(variantId).orElse(null);
            int physical = (inv != null && inv.getQuantity() != null) ? inv.getQuantity() : 0;

            if (physical < deductQty) {
                log.warn("Late payment finalization: Insufficient physical stock for variant #{} in order #{}. Physical: {}, Required: {}",
                        variantId, orderId, physical, deductQty);
                hasSufficientStock = false;
                break;
            }
        }

        if (!hasSufficientStock) {
            log.error("Cannot finalize inventory for order #{}: Insufficient stock due to expired hold. Cancelling order and flagging for refund.", orderId);
            String oldStatus = order.getOrderStatus() != null ? order.getOrderStatus() : "PAID";
            order.setOrderStatus("CANCELLED");
            orderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setOldStatus(oldStatus);
            history.setNewStatus("CANCELLED");
            history.setChangedAt(LocalDateTime.now());
            history.setRemark("LATE_PAYMENT_OUT_OF_STOCK: Payment confirmed, but stock was exhausted due to reservation expiry. Held for manual admin refund.");
            orderStatusHistoryRepository.save(history);

            reservationService.releaseReservationsForOrder(orderId);
            return false;
        }

        // 5. Consume active reservations for this order (RESERVED -> CONSUMED) and sync reserved_quantity
        reservationService.consumeReservationsForOrder(orderId);

        // 6. Delegate to inventory service to execute item deduction and SALE transaction logging
        inventoryService.deductStockForOrder(orderId);
        log.info("Successfully finalized inventory deduction for order #{}", orderId);
        return true;
    }
}
