package com.ojt.ecommerce.storefront.shipping.repository;
import com.ojt.ecommerce.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface G5ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderOrderId(Long orderId);
    java.util.List<com.ojt.ecommerce.entity.Shipment> findByOrderOrderIdIn(java.util.List<Long> orderIds);
}
