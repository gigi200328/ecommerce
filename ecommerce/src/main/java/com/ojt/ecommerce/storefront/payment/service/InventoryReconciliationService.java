package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryReconciliationService implements ApplicationRunner {

    private final G5OrderRepository orderRepository;
    private final OrderInventoryFinalizationService finalizationService;

    // Default lookback: last 24 hours to avoid processing unverified historical legacy orders
    @Value("${payment.reconciliation.lookback-hours:24}")
    private int lookbackHours = 24;

    // Batch size for pagination
    @Value("${payment.reconciliation.batch-size:50}")
    private int batchSize = 50;

    @Value("${payment.reconciliation.enabled:true}")
    private boolean reconciliationEnabled = true;

    /**
     * Executes on application startup to heal any unfinalized orders that may have been
     * interrupted by a JVM crash, deployment, or shutdown (Restart Recovery).
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!reconciliationEnabled) {
            log.info("Inventory reconciliation is disabled by configuration.");
            return;
        }
        log.info("Starting post-startup inventory reconciliation scan...");
        try {
            int reconciledCount = reconcileIncompleteOrders();
            log.info("Post-startup inventory reconciliation scan complete. Reconciled {} orders.", reconciledCount);
        } catch (Throwable t) {
            log.error("Error during startup inventory reconciliation: {}", t.getMessage(), t);
        }
    }

    /**
     * Periodic background reconciliation job.
     * Default schedule runs every 60 seconds with an initial delay of 30 seconds.
     */
    @Scheduled(fixedDelayString = "${payment.reconciliation.interval-ms:60000}", initialDelay = 30000)
    public void scheduledReconciliation() {
        if (!reconciliationEnabled) {
            return;
        }
        reconcileIncompleteOrders();
    }

    /**
     * Scans for paid orders that lack an inventory SALE transaction within the lookback window,
     * and finalizes inventory with row-lock synchronization and idempotency.
     *
     * @return count of successfully reconciled orders in this run
     */
    public int reconcileIncompleteOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(lookbackHours);
        PageRequest pageRequest = PageRequest.of(0, batchSize);

        List<Order> unfinalizedOrders = orderRepository.findPaidOrdersWithoutInventoryDeduction(cutoff, pageRequest);

        if (unfinalizedOrders == null || unfinalizedOrders.isEmpty()) {
            return 0;
        }

        log.info("Found {} unfinalized paid orders since {}. Initiating recovery...", unfinalizedOrders.size(), cutoff);
        int reconciledCount = 0;

        for (Order order : unfinalizedOrders) {
            Long orderId = order.getOrderId();
            try {
                // finalizationService acquires Order-level lock and rechecks SALE txn idempotency
                boolean success = finalizationService.finalizeInventory(orderId);
                if (success) {
                    reconciledCount++;
                }
            } catch (Throwable t) {
                log.error("Failed to reconcile inventory for order #{}: {}", orderId, t.getMessage(), t);
            }
        }

        return reconciledCount;
    }
}
