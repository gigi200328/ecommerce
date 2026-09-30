package com.ojt.ecommerce.storefront.shipping.repository;
import com.ojt.ecommerce.entity.DeliveryZone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface G5DeliveryZoneRepository extends JpaRepository<DeliveryZone, Long> {
    List<DeliveryZone> findByStatus(String status);
    Optional<DeliveryZone> findByCityAndTownshipAndStatus(String city, String township, String status);
    Optional<DeliveryZone> findByCityAndStatusAndTownshipIsNull(String city, String status);
}