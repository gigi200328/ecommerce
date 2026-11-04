package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryReconciliationServiceTest {

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private OrderInventoryFinalizationService finalizationService;

    @InjectMocks
    private InventoryReconciliationService reconciliationService;

    private Order order1;
    private Order order2;

    @BeforeEach
    void setUp() {
        order1 = Order.builder()
                .orderId(101L)
                .orderNo("ORD-101")
                .orderStatus("PAID")
                .paymentStatus("SUCCESS")
                .build();

        order2 = Order.builder()
                .orderId(102L)
                .orderNo("ORD-102")
                .orderStatus("PAID")
                .paymentStatus("SUCCESS")
                .build();

        ReflectionTestUtils.setField(reconciliationService, "lookbackHours", 24);
        ReflectionTestUtils.setField(reconciliationService, "batchSize", 50);
        ReflectionTestUtils.setField(reconciliationService, "reconciliationEnabled", true);
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Finds unfinalized paid orders and successfully finalizes all")
    void reconcileIncompleteOrders_WhenOrdersFound_ShouldFinalizeAllOrders() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1, order2));
        when(finalizationService.finalizeInventory(101L)).thenReturn(true);
        when(finalizationService.finalizeInventory(102L)).thenReturn(true);

        int reconciledCount = reconciliationService.reconcileIncompleteOrders();

        assertThat(reconciledCount).isEqualTo(2);
        verify(finalizationService).finalizeInventory(101L);
        verify(finalizationService).finalizeInventory(102L);
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Returns 0 when no unfinalized orders exist")
    void reconcileIncompleteOrders_WhenNoOrdersFound_ShouldReturnZero() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        int reconciledCount = reconciliationService.reconcileIncompleteOrders();

        assertThat(reconciledCount).isZero();
        verify(finalizationService, never()).finalizeInventory(anyLong());
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Returns 0 when repository returns null list")
    void reconcileIncompleteOrders_WhenNullOrdersList_ShouldReturnZero() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(null);

        int reconciledCount = reconciliationService.reconcileIncompleteOrders();

        assertThat(reconciledCount).isZero();
        verify(finalizationService, never()).finalizeInventory(anyLong());
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Does not count orders where finalization returns false")
    void reconcileIncompleteOrders_WhenFinalizationReturnsFalse_ShouldNotCountInReconciled() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1, order2));
        when(finalizationService.finalizeInventory(101L)).thenReturn(true);
        when(finalizationService.finalizeInventory(102L)).thenReturn(false);

        int reconciledCount = reconciliationService.reconcileIncompleteOrders();

        assertThat(reconciledCount).isEqualTo(1);
        verify(finalizationService).finalizeInventory(101L);
        verify(finalizationService).finalizeInventory(102L);
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Exception during finalization of one order does not stop others")
    void reconcileIncompleteOrders_WhenOneOrderThrowsException_ShouldContinueProcessingOthers() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1, order2));
        when(finalizationService.finalizeInventory(101L)).thenThrow(new RuntimeException("Transient DB lock"));
        when(finalizationService.finalizeInventory(102L)).thenReturn(true);

        int reconciledCount = reconciliationService.reconcileIncompleteOrders();

        assertThat(reconciledCount).isEqualTo(1);
        verify(finalizationService).finalizeInventory(101L);
        verify(finalizationService).finalizeInventory(102L);
    }

    @Test
    @DisplayName("run: Startup runner executes reconciliation when enabled")
    void run_WhenReconciliationEnabled_ShouldTriggerReconciliation() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1));
        when(finalizationService.finalizeInventory(101L)).thenReturn(true);

        reconciliationService.run(null);

        verify(finalizationService).finalizeInventory(101L);
    }

    @Test
    @DisplayName("run: Startup runner skips reconciliation when disabled by configuration")
    void run_WhenReconciliationDisabled_ShouldSkipReconciliation() {
        ReflectionTestUtils.setField(reconciliationService, "reconciliationEnabled", false);

        reconciliationService.run(null);

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(finalizationService);
    }

    @Test
    @DisplayName("run: Exception during startup reconciliation is caught and logged, does not crash app")
    void run_WhenReconciliationThrows_ShouldCatchAndNotPropagateException() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenThrow(new RuntimeException("Database down"));

        assertThatCode(() -> reconciliationService.run(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("scheduledReconciliation: Periodic job executes reconciliation when enabled")
    void scheduledReconciliation_WhenEnabled_ShouldTriggerReconciliation() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1));
        when(finalizationService.finalizeInventory(101L)).thenReturn(true);

        reconciliationService.scheduledReconciliation();

        verify(finalizationService).finalizeInventory(101L);
    }

    @Test
    @DisplayName("scheduledReconciliation: Periodic job skips execution when disabled")
    void scheduledReconciliation_WhenDisabled_ShouldSkipReconciliation() {
        ReflectionTestUtils.setField(reconciliationService, "reconciliationEnabled", false);

        reconciliationService.scheduledReconciliation();

        verifyNoInteractions(orderRepository);
        verifyNoInteractions(finalizationService);
    }
}
