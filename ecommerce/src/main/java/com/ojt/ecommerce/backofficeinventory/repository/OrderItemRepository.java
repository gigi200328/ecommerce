package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
	// ဆွဲထုတ်ရန် Showing exact product in exact order ID
	List<OrderItem> orderItemId(Long orderItemId);

	// Find all order items for an order
	List<OrderItem> findByOrder_OrderId(Long orderId);
}
