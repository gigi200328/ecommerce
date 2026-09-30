package com.ojt.ecommerce.storefront.checkout.repository;
import com.ojt.ecommerce.entity.OrderAddress;
import org.springframework.data.jpa.repository.JpaRepository;
public interface G5OrderAddressRepository extends JpaRepository<OrderAddress, Long> {
    java.util.Optional<com.ojt.ecommerce.entity.OrderAddress> findByOrderOrderId(Long orderId);
}
