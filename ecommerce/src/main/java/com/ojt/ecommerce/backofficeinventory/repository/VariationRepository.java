package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.Variation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VariationRepository extends JpaRepository<Variation, Long> {

	boolean existsByName(String name);

}