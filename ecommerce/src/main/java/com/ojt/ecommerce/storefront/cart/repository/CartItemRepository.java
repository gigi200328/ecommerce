package com.ojt.ecommerce.storefront.cart.repository;
import com.ojt.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartCartId(Long cartId);
    Optional<CartItem> findByCartItemIdAndCartCustomerCustomerId(Long cartItemId, Long customerId);
    Optional<CartItem> findByCartCartIdAndVariantVariantId(Long cartId, Long variantId);
}