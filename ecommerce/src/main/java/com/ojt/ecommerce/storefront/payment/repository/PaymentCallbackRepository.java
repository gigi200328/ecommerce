package com.ojt.ecommerce.storefront.payment.repository;
import com.ojt.ecommerce.entity.PaymentCallback;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentCallbackRepository extends JpaRepository<PaymentCallback, Long> {
    boolean existsByExternalEventId(String externalEventId);
}