package com.ojt.ecommerce.backofficeinventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Variation;

@Repository
public interface VariationRepository
        extends JpaRepository<Variation, Long> {

    boolean existsByName(String name);

}