package com.ojt.ecommerce.storefront.payment.repository;
import com.ojt.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface G5PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrderOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<Payment> findByTransactionRef(String transactionRef);
}