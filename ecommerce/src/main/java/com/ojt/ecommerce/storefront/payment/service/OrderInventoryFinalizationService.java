package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.backofficeinventory.repository.InventoryTransactionRepository;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderInventoryFinalizationService {

    private final G5OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final InventoryTransactionRepository inventoryTxnRepository;

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

        // 4. Delegate to inventory service to execute item deduction and SALE transaction logging
        inventoryService.deductStockForOrder(orderId);
        log.info("Successfully finalized inventory deduction for order #{}", orderId);
        return true;
    }
}
