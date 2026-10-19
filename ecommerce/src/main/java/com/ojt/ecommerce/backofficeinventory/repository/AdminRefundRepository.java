package com.ojt.ecommerce.backofficeinventory.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.RefundRequest;

@Repository
public interface AdminRefundRepository extends JpaRepository<RefundRequest, Long> {

    @Query("""
        SELECT r FROM RefundRequest r
        JOIN FETCH r.orderItem oi
        JOIN FETCH oi.order o
        JOIN FETCH o.customer c
        LEFT JOIN FETCH r.processedBy u
        WHERE (:status IS NULL OR :status = '' OR :status = 'ALL' OR UPPER(r.status) = UPPER(:status))
          AND (:startDateTime IS NULL OR r.requestedAt >= :startDateTime)
          AND (:endDateTime IS NULL OR r.requestedAt <= :endDateTime)
          AND (:search IS NULL OR :search = '' 
               OR LOWER(o.orderNo) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(oi.productName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.requestReason) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY r.requestedAt DESC
    """)
    List<RefundRequest> findRefundsWithFilters(
            @Param("search") String search,
            @Param("status") String status,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    @Query("SELECT COUNT(r) FROM RefundRequest r")
    long countTotalRequests();

    @Query("SELECT COUNT(r) FROM RefundRequest r WHERE UPPER(r.status) = 'PENDING'")
    long countPendingRequests();

    @Query("SELECT COUNT(r) FROM RefundRequest r WHERE UPPER(r.status) = 'APPROVED'")
    long countApprovedRequests();

    @Query("SELECT COUNT(r) FROM RefundRequest r WHERE UPPER(r.status) = 'REJECTED'")
    long countRejectedRequests();

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM RefundRequest r WHERE UPPER(r.status) = 'APPROVED'")
    BigDecimal sumApprovedRefundAmount();
}
