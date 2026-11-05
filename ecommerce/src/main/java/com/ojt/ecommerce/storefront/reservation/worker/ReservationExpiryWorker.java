package com.ojt.ecommerce.storefront.reservation.worker;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderStatusHistoryRepository;
import com.ojt.ecommerce.storefront.payment.repository.G5PaymentRepository;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryWorker {

    private final InventoryReservationService reservationService;
    private final G5PaymentRepository paymentRepository;
    private final G5OrderRepository orderRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Value("${storefront.inventory.reservation-expiry-enabled:true}")
    private boolean expiryWorkerEnabled = true;

    @Scheduled(fixedDelayString = "${storefront.inventory.reservation-expiry-interval-ms:60000}", initialDelay = 15000)
    public void runReservationExpirySweep() {
        if (!expiryWorkerEnabled) {
            return;
        }

        try {
            List<Long> expiredOrderIds = reservationService.expireAbandonedReservationsAndGetOrderIds();
            if (!expiredOrderIds.isEmpty()) {
                log.info("Inventory reservation expiry worker swept and found {} expired orders.", expiredOrderIds.size());
                for (Long orderId : expiredOrderIds) {
                    processExpiredOrder(orderId);
                }
            }
        } catch (Throwable t) {
            log.error("Error during scheduled inventory reservation expiry sweep: {}", t.getMessage(), t);
        }
    }

    @Transactional
    public void processExpiredOrder(Long orderId) {
        // 1. Critical payment-started guard: Pre-payment 3-minute expiry stops applying once payment is initiated
        if (paymentRepository.existsByOrderOrderId(orderId)) {
            log.info("Order #{} has already initiated payment. Skipping pre-payment expiry sweep.", orderId);
            return;
        }

        Order order = orderRepository.findByIdWithLock(orderId).orElse(null);
        if (order == null) {
            return;
        }

        // 2. Critical race protection: SUCCESS must win over EXPIRE!
        boolean isPaid = "PAID".equalsIgnoreCase(order.getOrderStatus())
                || "SUCCESS".equalsIgnoreCase(order.getPaymentStatus());
        if (isPaid) {
            log.info("Order #{} is already PAID/SUCCESS. Skipping expiration terminalization.", orderId);
            return;
        }

        // 3. Terminalize order as CANCELLED (native MySQL ENUM) with FAILED payment status
        if (!"CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            order.setOrderStatus("CANCELLED");
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setOldStatus("PENDING");
            history.setNewStatus("CANCELLED");
            history.setChangedAt(LocalDateTime.now());
            history.setRemark("Order cancelled due to reservation timeout");
            orderStatusHistoryRepository.save(history);
        }

        // 4. Cart A items DO NOT restore into active Cart B on expiry per revised business rules
        log.info("Order #{} expired without payment initiation. Stock released, order cancelled, no cart restoration.", orderId);
    }
}
