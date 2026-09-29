package com.ojt.ecommerce.storefront.checkout.repository;
import com.ojt.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
public interface G5OrderRepository extends JpaRepository<Order, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
}