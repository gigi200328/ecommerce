package com.ojt.ecommerce.storefront.checkout.repository;
import com.ojt.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface G5OrderItemRepository extends JpaRepository<OrderItem, Long> {
    java.util.List<com.ojt.ecommerce.entity.OrderItem> findByOrderOrderId(Long orderId);    java.util.List<com.ojt.ecommerce.entity.OrderItem> findByOrderOrderIdIn(java.util.List<Long> orderIds);
}
