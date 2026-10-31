package com.ojt.ecommerce.storefront.payment.listener;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.storefront.payment.event.OrderPaidEvent;
import com.ojt.ecommerce.storefront.payment.service.OrderInventoryFinalizationService;

@ExtendWith(MockitoExtension.class)
class OrderPaidEventListenerTest {

    @Mock
    private OrderInventoryFinalizationService finalizationService;

    @InjectMocks
    private OrderPaidEventListener eventListener;

    private OrderPaidEvent mockEvent;

    @BeforeEach
    void setUp() {
        mockEvent = new OrderPaidEvent(
                100L,
                "ORD-TEST-100",
                "TXN-TEST-100",
                BigDecimal.valueOf(25000),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("handleOrderPaid: AFTER_COMMIT listener delegates to OrderInventoryFinalizationService")
    void handleOrderPaid_delegatesToFinalizationService() {
        when(finalizationService.finalizeInventory(100L)).thenReturn(true);

        eventListener.handleOrderPaid(mockEvent);

        verify(finalizationService, times(1)).finalizeInventory(100L);
    }

    @Test
    @DisplayName("handleOrderPaid: Exception in finalization is caught and logged, never bubbling out of afterCommit")
    void handleOrderPaid_catchesAndIsolatesException() {
        when(finalizationService.finalizeInventory(100L))
                .thenThrow(new RuntimeException("Simulated database lock timeout during inventory deduction"));

        // Must not throw an exception (exception would bubble out of afterCommit and cause HTTP 500)
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> eventListener.handleOrderPaid(mockEvent));

        verify(finalizationService, times(1)).finalizeInventory(100L);
    }

    @Test
    @DisplayName("handleOrderPaid: Gracefully handles null event or null orderId")
    void handleOrderPaid_handlesNull() {
        eventListener.handleOrderPaid(null);
        eventListener.handleOrderPaid(new OrderPaidEvent(null, null, null, null, null));

        verify(finalizationService, never()).finalizeInventory(anyLong());
    }
}
