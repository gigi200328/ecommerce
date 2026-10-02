package com.ojt.ecommerce.storefront.shipping.repository;
import com.ojt.ecommerce.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
public interface G5ShippingRateRepository extends JpaRepository<ShippingRate, Long> {
    @Query("SELECT sr FROM ShippingRate sr WHERE sr.zone.zoneId = :zoneId AND sr.status = 'ACTIVE' " +
           "AND (sr.effectiveFrom IS NULL OR sr.effectiveFrom <= :now) " +
           "AND (sr.effectiveTo IS NULL OR sr.effectiveTo >= :now) " +
           "ORDER BY sr.shippingRateId DESC limit 1")
    Optional<ShippingRate> findActiveRateForZone(@Param("zoneId") Long zoneId, @Param("now") LocalDateTime now);
}