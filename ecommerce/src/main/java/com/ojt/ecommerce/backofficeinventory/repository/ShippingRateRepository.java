package com.ojt.ecommerce.backofficeinventory.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ojt.ecommerce.entity.ShippingRate;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {

	@Query("SELECT sr FROM ShippingRate sr WHERE sr.zone.zoneId = :zoneId AND sr.status = 'ACTIVE' "
			+ "AND (sr.effectiveFrom IS NULL OR sr.effectiveFrom <= :now) "
			+ "AND (sr.effectiveTo IS NULL OR sr.effectiveTo >= :now) " + "ORDER BY sr.shippingRateId DESC limit 1")
	Optional<ShippingRate> findCurrentRateByZoneId(@Param("zoneId") Long zoneId, @Param("now") LocalDateTime now);
}