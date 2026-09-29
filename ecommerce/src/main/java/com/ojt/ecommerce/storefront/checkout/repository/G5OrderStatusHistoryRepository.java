package com.ojt.ecommerce.storefront.checkout.repository;
import com.ojt.ecommerce.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
public interface G5OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    java.util.List<com.ojt.ecommerce.entity.OrderStatusHistory> findByOrderOrderIdOrderByChangedAtAsc(Long orderId);
}
