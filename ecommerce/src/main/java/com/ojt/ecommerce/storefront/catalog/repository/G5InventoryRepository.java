package com.ojt.ecommerce.storefront.catalog.repository;
import com.ojt.ecommerce.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface G5InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByVariantVariantId(Long variantId);
    List<Inventory> findByVariantVariantIdIn(List<Long> variantIds);
}