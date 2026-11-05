package com.ojt.ecommerce.storefront.reservation.repository;

import com.ojt.ecommerce.storefront.reservation.entity.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderOrderId(Long orderId);

    List<InventoryReservation> findByOrderOrderIdAndStatus(Long orderId, String status);

    Optional<InventoryReservation> findByOrderOrderIdAndVariantVariantId(Long orderId, Long variantId);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM InventoryReservation r " +
           "WHERE r.variant.variantId = :variantId AND r.status = 'RESERVED' AND r.expiresAt > :now")
    int sumActiveReservedQuantityByVariant(@Param("variantId") Long variantId, @Param("now") LocalDateTime now);

    @Query("SELECT r.variant.variantId, COALESCE(SUM(r.quantity), 0) FROM InventoryReservation r " +
           "WHERE r.variant.variantId IN :variantIds AND r.status = 'RESERVED' AND r.expiresAt > :now " +
           "GROUP BY r.variant.variantId")
    List<Object[]> sumActiveReservedQuantityByVariantIds(@Param("variantIds") List<Long> variantIds, @Param("now") LocalDateTime now);

    @Query("SELECT r FROM InventoryReservation r WHERE r.status = 'RESERVED' AND r.expiresAt <= :now")
    List<InventoryReservation> findExpiredReservations(@Param("now") LocalDateTime now);
}
