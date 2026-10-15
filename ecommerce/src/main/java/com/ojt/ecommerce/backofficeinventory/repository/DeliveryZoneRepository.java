package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ojt.ecommerce.entity.DeliveryZone;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {
	Optional<DeliveryZone> findByCityAndTownship(String city, String township);

	Optional<DeliveryZone> findByCityAndTownshipIsNull(String city);

	List<DeliveryZone> findAllByOrderByZoneIdAsc();
}
