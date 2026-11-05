package com.ojt.ecommerce.storefront.catalog.repository;

import com.ojt.ecommerce.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface G5InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariantVariantId(Long variantId);
    List<Inventory> findByVariantVariantIdIn(List<Long> variantIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.variant.variantId = :variantId")
    Optional<Inventory> findByVariantIdForUpdate(@Param("variantId") Long variantId);
}