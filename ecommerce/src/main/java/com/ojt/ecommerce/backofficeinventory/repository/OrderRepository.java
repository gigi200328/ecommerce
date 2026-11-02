package com.ojt.ecommerce.backofficeinventory.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	// Incoming Orders
	Page<Order> findByOrderStatus(String orderStatus, Pageable pageable);

	// Show latest order depending on order time
	List<Order> findAllByOrderByCreatedAtDesc();

	// Find order by order no.
	Optional<Order> findByOrderNo(String orderNo);

	// Find orders within date range for reports
	List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

	@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderItems oi LEFT JOIN FETCH oi.variant v LEFT JOIN FETCH v.product p WHERE o.createdAt BETWEEN :start AND :end")
	List<Order> findWithItemsByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
