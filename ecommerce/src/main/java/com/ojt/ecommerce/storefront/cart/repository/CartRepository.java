package com.ojt.ecommerce.storefront.cart.repository;
import com.ojt.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerCustomerIdAndStatus(Long customerId, String status);
}