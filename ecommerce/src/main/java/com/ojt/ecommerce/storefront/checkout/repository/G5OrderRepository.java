package com.ojt.ecommerce.storefront.checkout.repository;

import com.ojt.ecommerce.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface G5OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithLock(@Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'PAID' " +
           "AND o.createdAt >= :since " +
           "AND o.orderItems IS NOT EMPTY " +
           "AND NOT EXISTS (" +
           "    SELECT it FROM InventoryTransaction it " +
           "    WHERE it.txnType = 'SALE' AND it.referenceType = 'ORDER' AND it.referenceId = o.orderId" +
           ") ORDER BY o.orderId ASC")
    List<Order> findPaidOrdersWithoutInventoryDeduction(@Param("since") LocalDateTime since, Pageable pageable);
}
