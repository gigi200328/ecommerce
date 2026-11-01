package com.ojt.ecommerce.storefront.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;

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
        order1 = Order.builder().orderId(101L).orderNo("ORD-101").orderStatus("PAID").build();
        order2 = Order.builder().orderId(102L).orderNo("ORD-102").orderStatus("PAID").build();
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Finds unfinalized paid orders and finalizes them")
    void reconcileIncompleteOrders_findsAndFinalizesOrders() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1, order2));
        when(finalizationService.finalizeInventory(101L)).thenReturn(true);
        when(finalizationService.finalizeInventory(102L)).thenReturn(true);

        int count = reconciliationService.reconcileIncompleteOrders();

        assertEquals(2, count);
        verify(finalizationService, times(1)).finalizeInventory(101L);
        verify(finalizationService, times(1)).finalizeInventory(102L);
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Returns 0 when no unfinalized orders exist")
    void reconcileIncompleteOrders_handlesEmptyList() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        int count = reconciliationService.reconcileIncompleteOrders();

        assertEquals(0, count);
        verify(finalizationService, never()).finalizeInventory(anyLong());
    }

    @Test
    @DisplayName("reconcileIncompleteOrders: Partial failure does not abort remaining orders")
    void reconcileIncompleteOrders_handlesPartialErrors() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1, order2));
        when(finalizationService.finalizeInventory(101L)).thenThrow(new RuntimeException("Transient DB error"));
        when(finalizationService.finalizeInventory(102L)).thenReturn(true);

        int count = reconciliationService.reconcileIncompleteOrders();

        assertEquals(1, count);
        verify(finalizationService, times(1)).finalizeInventory(101L);
        verify(finalizationService, times(1)).finalizeInventory(102L);
    }

    @Test
    @DisplayName("run: ApplicationRunner triggers startup reconciliation for crash recovery")
    void run_executesStartupReconciliation() {
        when(orderRepository.findPaidOrdersWithoutInventoryDeduction(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(order1));
        when(finalizationService.finalizeInventory(101L)).thenReturn(true);

        reconciliationService.run(null);

        verify(finalizationService, times(1)).finalizeInventory(101L);
    }
}
