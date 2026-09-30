package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	// Incoming Orders
	List<Order> findByOrderStatus(String orderStatus);

	// Show latest order depending on order time
	List<Order> findAllByOrderByCreatedAtDesc();

	// Find order by order no.
	Optional<Order> findByOrderNo(String orderNo);
}