package com.ojt.ecommerce.storefront.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ojt.ecommerce.backofficeinventory.repository.InventoryTransactionRepository;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderInventoryFinalizationServiceTest {

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryTransactionRepository inventoryTxnRepository;

    @InjectMocks
    private OrderInventoryFinalizationService finalizationService;

    private Order mockOrder;

    @BeforeEach
    void setUp() {
        mockOrder = Order.builder()
                .orderId(100L)
                .orderNo("ORD-TEST-100")
                .orderStatus("PAID")
                .paymentStatus("SUCCESS")
                .build();

        OrderItem mockItem = OrderItem.builder()
                .orderItemId(1L)
                .order(mockOrder)
                .qty(2)
                .build();
        mockOrder.setOrderItems(List.of(mockItem));
    }

    @Test
    @DisplayName("finalizeInventory: Acquires Order-level lock and invokes inventoryService when not already finalized")
    void finalizeInventory_acquiresLock_andDeductsStock() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(inventoryTxnRepository.existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", 100L))
                .thenReturn(false);

        boolean result = finalizationService.finalizeInventory(100L);

        assertTrue(result);
        verify(orderRepository, times(1)).findByIdWithLock(100L);
        verify(inventoryTxnRepository, times(1)).existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", 100L);
        verify(inventoryService, times(1)).deductStockForOrder(100L);
    }

    @Test
    @DisplayName("finalizeInventory: Idempotent - Skips deduction when SALE transaction already exists")
    void finalizeInventory_skips_whenAlreadyFinalized() {
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));
        when(inventoryTxnRepository.existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", 100L))
                .thenReturn(true);

        boolean result = finalizationService.finalizeInventory(100L);

        assertTrue(result);
        verify(orderRepository, times(1)).findByIdWithLock(100L);
        verify(inventoryService, never()).deductStockForOrder(anyLong());
    }

    @Test
    @DisplayName("finalizeInventory: Skips when Order has zero items (empty or null list)")
    void finalizeInventory_skips_whenOrderHasZeroItems() {
        mockOrder.setOrderItems(Collections.emptyList());

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        boolean result = finalizationService.finalizeInventory(100L);

        assertFalse(result);
        verify(orderRepository, times(1)).findByIdWithLock(100L);
        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(inventoryTxnRepository, never()).existsByTxnTypeAndReferenceTypeAndReferenceId(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("finalizeInventory: Skips when Order is not in PAID/SUCCESS status")
    void finalizeInventory_skips_whenOrderNotPaid() {
        mockOrder.setOrderStatus("PENDING");
        mockOrder.setPaymentStatus("PENDING");

        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(mockOrder));

        boolean result = finalizationService.finalizeInventory(100L);

        assertFalse(result);
        verify(inventoryService, never()).deductStockForOrder(anyLong());
        verify(inventoryTxnRepository, never()).existsByTxnTypeAndReferenceTypeAndReferenceId(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("finalizeInventory: Gracefully handles non-existent order")
    void finalizeInventory_handlesNotFound() {
        when(orderRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        boolean result = finalizationService.finalizeInventory(999L);

        assertFalse(result);
        verify(inventoryService, never()).deductStockForOrder(anyLong());
    }

    @Test
    @DisplayName("finalizeInventory: Handles null orderId safely")
    void finalizeInventory_handlesNull() {
        boolean result = finalizationService.finalizeInventory(null);
        assertFalse(result);
        verify(orderRepository, never()).findByIdWithLock(anyLong());
    }
}
