package com.ojt.ecommerce.storefront.inventory.repository;
import com.ojt.ecommerce.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MockInventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}