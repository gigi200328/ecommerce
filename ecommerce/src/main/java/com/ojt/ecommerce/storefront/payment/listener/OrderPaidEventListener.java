package com.ojt.ecommerce.storefront.payment.listener;

import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import com.ojt.ecommerce.storefront.payment.service.OrderInventoryFinalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidEventListener {

    private final OrderInventoryFinalizationService finalizationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaid(OrderPaidEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }
        log.info("Handling OrderPaidEvent AFTER_COMMIT for order #{}", event.orderId());
        try {
            finalizationService.finalizeInventory(event.orderId());
        } catch (Throwable t) {
            // Crucial C1 boundary: The payment and order records have already committed in MySQL.
            // Catch and log the failure so that no exception escapes afterCommit() to corrupt the
            // webhook HTTP response. Unfinalized inventory is durably self-healed by the reconciler.
            log.error("Post-commit inventory finalization failed for order #{}: {}. Order will be recovered by reconciler.",
                    event.orderId(), t.getMessage(), t);
        }
    }
}
