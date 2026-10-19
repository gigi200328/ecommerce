package com.ojt.ecommerce.backofficeinventory.seeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.repository.AdminRefundRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderItemRepository;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.RefundRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Automatically seeds 5 sample Refund Requests (PENDING, APPROVED, REJECTED)
 * on application startup if the refund_requests table is currently empty.
 * This guarantees the user can immediately test the Admin UI table, Approve/Reject buttons,
 * and Dynamic PDF/Excel exports.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class RefundDataSeeder implements CommandLineRunner {

    private final AdminRefundRepository adminRefundRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (adminRefundRepository.count() > 0) {
            log.info("Refund table already contains {} records. Skipping sample refund seeding.", adminRefundRepository.count());
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findAll();
        if (orderItems.isEmpty()) {
            log.info("No order items found in database yet. Seed data in data.sql or place orders first.");
            return;
        }

        log.info("Seeding 5 sample Refund Requests for Admin Refund Management testing...");

        OrderItem firstItem = orderItems.get(0);
        OrderItem secondItem = orderItems.size() > 1 ? orderItems.get(1) : firstItem;
        OrderItem thirdItem = orderItems.size() > 2 ? orderItems.get(2) : firstItem;

        // 1. PENDING - DAMAGED_ITEM
        RefundRequest r1 = RefundRequest.builder()
                .orderItem(firstItem)
                .refundQuantity(1)
                .requestReason("DAMAGED_ITEM")
                .refundAmount(firstItem.getUnitPrice() != null ? firstItem.getUnitPrice() : BigDecimal.valueOf(45.00))
                .status("PENDING")
                .requestedAt(LocalDateTime.now().minusDays(2))
                .remark(null)
                .build();
        adminRefundRepository.save(r1);

        // 2. PENDING - DEFECTIVE_PRODUCT
        RefundRequest r2 = RefundRequest.builder()
                .orderItem(secondItem)
                .refundQuantity(2)
                .requestReason("DEFECTIVE_PRODUCT")
                .refundAmount(secondItem.getSubtotal() != null ? secondItem.getSubtotal() : BigDecimal.valueOf(120.00))
                .status("PENDING")
                .requestedAt(LocalDateTime.now().minusDays(1))
                .remark(null)
                .build();
        adminRefundRepository.save(r2);

        // 3. APPROVED - WRONG_ITEM_SENT
        RefundRequest r3 = RefundRequest.builder()
                .orderItem(thirdItem)
                .refundQuantity(1)
                .requestReason("WRONG_ITEM_SENT")
                .refundAmount(thirdItem.getUnitPrice() != null ? thirdItem.getUnitPrice() : BigDecimal.valueOf(89.50))
                .status("APPROVED")
                .requestedAt(LocalDateTime.now().minusDays(5))
                .processedAt(LocalDateTime.now().minusDays(3))
                .remark("Refund approved. Incorrect color variant sent by fulfillment center.")
                .build();
        adminRefundRepository.save(r3);

        // 4. APPROVED - ORDERED_BY_MISTAKE
        RefundRequest r4 = RefundRequest.builder()
                .orderItem(firstItem)
                .refundQuantity(1)
                .requestReason("ORDERED_BY_MISTAKE")
                .refundAmount(firstItem.getUnitPrice() != null ? firstItem.getUnitPrice() : BigDecimal.valueOf(250.00))
                .status("APPROVED")
                .requestedAt(LocalDateTime.now().minusDays(7))
                .processedAt(LocalDateTime.now().minusDays(6))
                .remark("Approved prior to dispatch upon customer immediate request.")
                .build();
        adminRefundRepository.save(r4);

        // 5. REJECTED - MISSING_PARTS
        RefundRequest r5 = RefundRequest.builder()
                .orderItem(secondItem)
                .refundQuantity(1)
                .requestReason("MISSING_PARTS")
                .refundAmount(secondItem.getUnitPrice() != null ? secondItem.getUnitPrice() : BigDecimal.valueOf(65.00))
                .status("REJECTED")
                .requestedAt(LocalDateTime.now().minusDays(4))
                .processedAt(LocalDateTime.now().minusDays(2))
                .remark("Rejection note: Warehouse dispatch security footage verified all accessories were included.")
                .build();
        adminRefundRepository.save(r5);

        log.info("Successfully seeded 5 sample Refund Requests (2 PENDING, 2 APPROVED, 1 REJECTED).");
    }
}
