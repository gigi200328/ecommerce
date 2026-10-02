package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
	@Query("SELECT p FROM Payment p JOIN FETCH p.order WHERE p.order.orderNo = :orderNo")
	Optional<Payment> findByOrder_OrderNo(String orderNo);
}