package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.backofficeinventory.repository.InventoryTransactionRepository;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderInventoryFinalizationServiceTest {

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private InventoryTransactionRepository inventoryTxnRepository;

    @Mock
    private InventoryReservationService reservationService;

    @InjectMocks
    private OrderInventoryFinalizationService finalizationService;

    @Test
    void finalizeInventory_WhenOrderIdNull_ShouldReturnFalse() {
        boolean result = finalizationService.finalizeInventory(null);
        assertThat(result).isFalse();
        verifyNoInteractions(orderRepository);
    }

    @Test
    void finalizeInventory_WhenOrderNotFound_ShouldReturnFalse() {
        when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        boolean result = finalizationService.finalizeInventory(1L);

        assertThat(result).isFalse();
        verify(orderRepository).findByIdWithLock(1L);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void finalizeInventory_WhenOrderNotPaidOrSuccess_ShouldReturnFalse() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PENDING");

        when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));

        boolean result = finalizationService.finalizeInventory(1L);

        assertThat(result).isFalse();
        verify(inventoryService, never()).deductStockForOrder(anyLong());
    }

    @Test
    void finalizeInventory_WhenOrderHasNoItems_ShouldReturnFalse() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderStatus("PAID");
        order.setOrderItems(List.of());

        when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));

        boolean result = finalizationService.finalizeInventory(1L);

        assertThat(result).isFalse();
        verify(inventoryService, never()).deductStockForOrder(anyLong());
    }

    @Test
    void finalizeInventory_WhenAlreadyDeducted_ShouldReturnTrueWithoutDeductingAgain() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOrderStatus("PAID");
        order.setOrderItems(List.of(new OrderItem()));

        when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));
        when(inventoryTxnRepository.existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", 1L))
                .thenReturn(true);

        boolean result = finalizationService.finalizeInventory(1L);

        assertThat(result).isTrue();
        verify(inventoryService, never()).deductStockForOrder(anyLong());
    }

    @Test
    void finalizeInventory_WhenNotYetDeducted_ShouldExecuteDeductionAndReturnTrue() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setPaymentStatus("SUCCESS");
        order.setOrderItems(List.of(new OrderItem()));

        when(orderRepository.findByIdWithLock(1L)).thenReturn(Optional.of(order));
        when(inventoryTxnRepository.existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", 1L))
                .thenReturn(false);

        boolean result = finalizationService.finalizeInventory(1L);

        assertThat(result).isTrue();
        verify(reservationService).consumeReservationsForOrder(1L);
        verify(inventoryService).deductStockForOrder(1L);
    }
}