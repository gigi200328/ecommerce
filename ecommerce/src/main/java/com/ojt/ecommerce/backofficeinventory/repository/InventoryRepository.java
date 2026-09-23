package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.Inventory;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Find inventory for a specific variant
    Optional<Inventory> findByVariant_VariantId(Long variantId);

    // Check if inventory exists for a variant
    boolean existsByVariant_VariantId(Long variantId);

    // Find by stock status (NORMAL, LOW_STOCK, OUT_OF_STOCK)
    List<Inventory> findByStatus(String status);

    // Find low stock items (quantity <= reorderLevel or status is LOW_STOCK/OUT_OF_STOCK)
    @Query("SELECT i FROM Inventory i JOIN FETCH i.variant v JOIN FETCH v.product p WHERE i.quantity <= i.reorderLevel OR i.status IN ('LOW_STOCK', 'OUT_OF_STOCK')")
    List<Inventory> findLowStockInventories();
}
