package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ojt.ecommerce.entity.DeliveryZone;

public interface DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {
	Optional<DeliveryZone> findByCityAndTownship(String city, String township);

	Optional<DeliveryZone> findByCityAndTownshipIsNull(String city);

	List<DeliveryZone> findAllByOrderByZoneIdAsc();

	boolean existsByZoneNameIgnoreCase(String zoneName);

	// ၂။ သက်ဆိုင်ရာ City ထဲတွင် အဆိုပါ Township သတ်မှတ်ပြီးသား ရှိမရှိ စစ်ဆေးရန်
	boolean existsByCityAndTownship(String city, String township);
}
