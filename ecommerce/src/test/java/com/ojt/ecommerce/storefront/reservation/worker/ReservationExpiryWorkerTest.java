package com.ojt.ecommerce.storefront.reservation.worker;

import com.ojt.ecommerce.entity.Cart;
import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.storefront.cart.service.CartService;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationExpiryWorkerTest {

    @Mock
    private InventoryReservationService reservationService;

    @Mock
    private CartService cartService;

    @Mock
    private G5OrderRepository orderRepository;

    @Mock
    private G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    @InjectMocks
    private ReservationExpiryWorker worker;

    private Customer customer;
    private Cart cartA;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(1L)
                .email("test@example.com")
                .fullName("Test Customer")
                .build();

        cartA = Cart.builder()
                .cartId(10L)
                .customer(customer)
                .status("PENDING_PAYMENT")
                .build();
    }

    @Test
    @DisplayName("runReservationExpirySweep: No expired reservations does nothing")
    void runSweep_noExpiredReservations_doesNothing() {
        when(reservationService.expireAbandonedReservationsAndGetOrderIds()).thenReturn(Collections.emptyList());

        worker.runReservationExpirySweep();

        verify(orderRepository, never()).findByIdWithLock(anyLong());
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("runReservationExpirySweep: CART order with expired reservation is cancelled and auto-restored")
    void runSweep_cartOrder_cancelledAndAutoRestored() {
        Order order = Order.builder()
                .orderId(100L)
                .customer(customer)
                .cart(cartA)
                .orderStatus("PENDING")
                .paymentStatus("PENDING")
                .checkoutType("CART")
                .totalAmount(new BigDecimal("50000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(reservationService.expireAbandonedReservationsAndGetOrderIds()).thenReturn(List.of(100L));
        when(orderRepository.findByIdWithLock(100L)).thenReturn(Optional.of(order));

        worker.runReservationExpirySweep();

        assertThat(order.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(order.getPaymentStatus()).isEqualTo("FAILED");
        verify(orderRepository).save(order);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(cartService).restoreFailedCartOrder(1L, 100L);
    }

    @Test
    @DisplayName("runReservationExpirySweep: Already PAID/SUCCESS order is not cancelled and not restored")
    void runSweep_alreadyPaidOrder_preserved() {
        Order paidOrder = Order.builder()
                .orderId(101L)
                .customer(customer)
                .cart(cartA)
                .orderStatus("PAID")
                .paymentStatus("SUCCESS")
                .checkoutType("CART")
                .build();

        when(reservationService.expireAbandonedReservationsAndGetOrderIds()).thenReturn(List.of(101L));
        when(orderRepository.findByIdWithLock(101L)).thenReturn(Optional.of(paidOrder));

        worker.runReservationExpirySweep();

        assertThat(paidOrder.getOrderStatus()).isEqualTo("PAID");
        assertThat(paidOrder.getPaymentStatus()).isEqualTo("SUCCESS");
        verify(orderRepository, never()).save(paidOrder);
        verify(orderStatusHistoryRepository, never()).save(any());
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }

    @Test
    @DisplayName("runReservationExpirySweep: BUY_NOW order cancelled without cart restore")
    void runSweep_buyNowOrder_cancelledWithoutCartRestore() {
        Order buyNowOrder = Order.builder()
                .orderId(102L)
                .customer(customer)
                .cart(null)
                .orderStatus("PENDING")
                .paymentStatus("PENDING")
                .checkoutType("BUY_NOW")
                .build();

        when(reservationService.expireAbandonedReservationsAndGetOrderIds()).thenReturn(List.of(102L));
        when(orderRepository.findByIdWithLock(102L)).thenReturn(Optional.of(buyNowOrder));

        worker.runReservationExpirySweep();

        assertThat(buyNowOrder.getOrderStatus()).isEqualTo("CANCELLED");
        assertThat(buyNowOrder.getPaymentStatus()).isEqualTo("FAILED");
        verify(orderRepository).save(buyNowOrder);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(cartService, never()).restoreFailedCartOrder(anyLong(), anyLong());
    }
}
