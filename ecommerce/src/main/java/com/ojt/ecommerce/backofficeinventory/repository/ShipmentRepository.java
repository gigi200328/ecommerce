package com.ojt.ecommerce.backofficeinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Shipment;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

}
