package com.ojt.ecommerce.backofficeinventory.seeder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.repository.AdminNotificationRepository;
import com.ojt.ecommerce.backofficeinventory.repository.AdminRefundRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderItemRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.AdminNotification;
import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.RefundRequest;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Automatically seeds sample Refund Requests (PENDING, APPROVED, REJECTED)
 * and corresponding customers, orders, order items and admin notifications
 * on application startup if the refund_requests table is currently empty.
 * This guarantees the user can immediately test the Admin UI table, Approve/Reject buttons,
 * and Dynamic PDF/Excel exports.
 */
@Slf4j
@Component
@org.springframework.core.annotation.Order(3)
@RequiredArgsConstructor
public class RefundDataSeeder implements CommandLineRunner {

    private final AdminRefundRepository adminRefundRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedMockData(false);
    }

    @Transactional
    public void seedMockData(boolean force) {
        if (!force && adminRefundRepository.count() > 0) {
            log.info("Refund table already contains {} records. Skipping sample refund seeding.", adminRefundRepository.count());
            return;
        }

        log.info("Seeding realistic mock data for Refund Management & Notifications...");

        // 1. Ensure Customers exist
        Customer c1 = getOrCreateCustomer("Aung Kyaw Thu", "aungkyawthu@gmail.com", "09971234567");
        Customer c2 = getOrCreateCustomer("Su Myat Noe", "sumyatnoe@gmail.com", "09781234568");
        Customer c3 = getOrCreateCustomer("Zaw Lin Htet", "zawlinhtet@gmail.com", "09451234569");
        Customer c4 = getOrCreateCustomer("Thiri Shwe Sin", "thirishwesin@gmail.com", "09261234570");
        Customer c5 = getOrCreateCustomer("Min Khant Ko", "minkhantko@gmail.com", "09961234571");

        // 2. Ensure ProductVariant exists
        List<ProductVariant> variants = productVariantRepository.findAll();
        ProductVariant variant1 = !variants.isEmpty() ? variants.get(0) : null;
        ProductVariant variant2 = variants.size() > 1 ? variants.get(1) : variant1;
        ProductVariant variant3 = variants.size() > 2 ? variants.get(2) : variant1;

        if (variant1 == null) {
            log.warn("No product variants found in database. Please ensure product catalog is seeded first.");
            return;
        }

        // 3. Ensure Orders and OrderItems exist
        OrderItem item1 = getOrCreateOrderItem("ORD-20261015-001", c1, variant1, "iPhone 15 Pro Max", 1, BigDecimal.valueOf(1199.00), "DELIVERED", "SUCCESS", LocalDateTime.now().minusDays(5));
        OrderItem item2 = getOrCreateOrderItem("ORD-20261016-002", c2, variant2, "MacBook Pro 16", 1, BigDecimal.valueOf(2499.00), "DELIVERED", "SUCCESS", LocalDateTime.now().minusDays(4));
        OrderItem item3 = getOrCreateOrderItem("ORD-20261017-003", c3, variant3, "AirPods Pro 2", 1, BigDecimal.valueOf(249.00), "DELIVERED", "REFUNDED", LocalDateTime.now().minusDays(6));
        OrderItem item4 = getOrCreateOrderItem("ORD-20261018-004", c4, variant1, "iPhone 15 Pro Max", 1, BigDecimal.valueOf(1199.00), "DELIVERED", "PARTIALLY_REFUNDED", LocalDateTime.now().minusDays(7));
        OrderItem item5 = getOrCreateOrderItem("ORD-20261019-005", c5, variant2, "MacBook Pro 16", 1, BigDecimal.valueOf(2499.00), "DELIVERED", "SUCCESS", LocalDateTime.now().minusDays(2));
        OrderItem item6 = getOrCreateOrderItem("ORD-20261020-006", c1, variant3, "AirPods Pro 2", 1, BigDecimal.valueOf(249.00), "DELIVERED", "SUCCESS", LocalDateTime.now().minusDays(8));

        User adminUser = userRepository.findAll().stream().findFirst().orElse(null);

        // 4. Seed Refund Requests if empty or forced
        if (force || adminRefundRepository.count() == 0) {
            // (1) PENDING - DEFECTIVE_PRODUCT
            saveRefund(item1, 1, "DEFECTIVE_PRODUCT", BigDecimal.valueOf(1199.00), "PENDING", LocalDateTime.now().minusDays(1), null, null, null);

            // (2) PENDING - DAMAGED_ITEM
            saveRefund(item2, 1, "DAMAGED_ITEM", BigDecimal.valueOf(2499.00), "PENDING", LocalDateTime.now().minusDays(2), null, null, null);

            // (3) PENDING - WRONG_ITEM_SENT
            saveRefund(item5, 1, "WRONG_ITEM_SENT", BigDecimal.valueOf(2499.00), "PENDING", LocalDateTime.now().minusHours(3), null, null, null);

            // (4) APPROVED - MISSING_PARTS
            saveRefund(item3, 1, "MISSING_PARTS", BigDecimal.valueOf(249.00), "APPROVED", LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3), adminUser, "Approved: Returned original box with missing charging cable. Refund issued to customer.");

            // (5) APPROVED - ORDERED_BY_MISTAKE
            saveRefund(item6, 1, "ORDERED_BY_MISTAKE", BigDecimal.valueOf(249.00), "APPROVED", LocalDateTime.now().minusDays(6), LocalDateTime.now().minusDays(4), adminUser, "Approved: Unopened packaging received and verified.");

            // (6) REJECTED - OTHER
            saveRefund(item4, 1, "OTHER", BigDecimal.valueOf(1199.00), "REJECTED", LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(2), adminUser, "Rejected: Return period expired (> 14 days) and physical modification detected.");

            // (7) REJECTED - DEFECTIVE_PRODUCT
            saveRefund(item6, 1, "DEFECTIVE_PRODUCT", BigDecimal.valueOf(249.00), "REJECTED", LocalDateTime.now().minusDays(7), LocalDateTime.now().minusDays(5), adminUser, "Rejected: Quality assessment confirmed normal functionality; no hardware defect found.");

            log.info("Successfully seeded 7 mock refund requests (3 PENDING, 2 APPROVED, 2 REJECTED).");
        }

        // 5. Seed sample notifications if empty
        if (adminNotificationRepository.count() == 0) {
            adminNotificationRepository.save(AdminNotification.builder()
                    .title("New Refund Request: ORD-20261015-001")
                    .message("Aung Kyaw Thu requested a refund of $1,199.00 for iPhone 15 Pro Max. Reason: DEFECTIVE_PRODUCT")
                    .type("REFUND_REQUEST")
                    .referenceId(101L)
                    .link("/admin/refunds")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusDays(1))
                    .build());

            adminNotificationRepository.save(AdminNotification.builder()
                    .title("New Refund Request: ORD-20261016-002")
                    .message("Su Myat Noe requested a refund of $2,499.00 for MacBook Pro 16. Reason: DAMAGED_ITEM")
                    .type("REFUND_REQUEST")
                    .referenceId(102L)
                    .link("/admin/refunds")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .build());

            adminNotificationRepository.save(AdminNotification.builder()
                    .title("New Refund Request: ORD-20261019-005")
                    .message("Min Khant Ko requested a refund of $2,499.00 for MacBook Pro 16. Reason: WRONG_ITEM_SENT")
                    .type("REFUND_REQUEST")
                    .referenceId(103L)
                    .link("/admin/refunds")
                    .isRead(false)
                    .createdAt(LocalDateTime.now().minusHours(3))
                    .build());

            adminNotificationRepository.save(AdminNotification.builder()
                    .title("Refund Approved: ORD-20261017-003")
                    .message("Zaw Lin Htet refund of $249.00 for AirPods Pro 2 was APPROVED.")
                    .type("REFUND_REQUEST")
                    .referenceId(104L)
                    .link("/admin/refunds")
                    .isRead(true)
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .build());
        }
    }

    private Customer getOrCreateCustomer(String name, String email, String phone) {
        return customerRepository.findByEmail(email).orElseGet(() -> {
            Customer c = Customer.builder()
                    .fullName(name)
                    .email(email)
                    .phone(phone)
                    .passwordHash("$2a$10$wK1Rk2eQ0wZ8H7X5G.YJkeWj8F2P3T3R4V6B8N0M2L4K6J8H0F2D")
                    .status("ACTIVE")
                    .failedLoginAttempts(0)
                    .createdAt(LocalDateTime.now().minusDays(30))
                    .build();
            return customerRepository.save(c);
        });
    }

    private OrderItem getOrCreateOrderItem(String orderNo, Customer customer, ProductVariant variant,
                                           String productName, int qty, BigDecimal unitPrice,
                                           String orderStatus, String paymentStatus, LocalDateTime createdAt) {
        Order order = orderRepository.findByOrderNo(orderNo).orElseGet(() -> {
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            Order newOrder = Order.builder()
                    .orderNo(orderNo)
                    .customer(customer)
                    .subtotalAmount(subtotal)
                    .discountAmount(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .totalAmount(subtotal)
                    .orderStatus(orderStatus)
                    .paymentStatus(paymentStatus)
                    .createdAt(createdAt)
                    .build();
            return orderRepository.save(newOrder);
        });

        List<OrderItem> existing = orderItemRepository.findByOrder(order);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        OrderItem item = OrderItem.builder()
                .order(order)
                .variant(variant)
                .productName(productName)
                .qty(qty)
                .unitPrice(unitPrice)
                .subtotal(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .build();
        return orderItemRepository.save(item);
    }

    private void saveRefund(OrderItem item, int qty, String reason, BigDecimal amount, String status,
                            LocalDateTime requestedAt, LocalDateTime processedAt, User processedBy, String remark) {
        RefundRequest refund = RefundRequest.builder()
                .orderItem(item)
                .refundQuantity(qty)
                .requestReason(reason)
                .refundAmount(amount)
                .status(status)
                .requestedAt(requestedAt)
                .processedAt(processedAt)
                .processedBy(processedBy)
                .remark(remark)
                .build();
        adminRefundRepository.save(refund);
    }
}
