package com.ojt.ecommerce.backofficeinventory.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ojt.ecommerce.entity.InventoryTransaction;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    // Find transactions by variant ordered by latest first
    List<InventoryTransaction> findByVariant_VariantIdOrderByCreatedAtDesc(Long variantId);

    // Find transactions by transaction type (RESTOCK, SALE, RETURN, WRITE_OFF, ADJUSTMENT)
    List<InventoryTransaction> findByTxnTypeOrderByCreatedAtDesc(String txnType);

    // Find all transactions ordered by latest first
    List<InventoryTransaction> findAllByOrderByCreatedAtDesc();
}
