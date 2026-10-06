package com.ojt.ecommerce.storefront.refund.repository;
import com.ojt.ecommerce.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByOrderItemOrderCustomerCustomerIdOrderByRequestedAtDesc(Long customerId);
    
    @Query("SELECT COALESCE(SUM(r.refundQuantity), 0) FROM RefundRequest r WHERE r.orderItem.orderItemId = :orderItemId AND r.status IN ('PENDING', 'APPROVED')")
    Integer getReservedQuantity(@Param("orderItemId") Long orderItemId);
}