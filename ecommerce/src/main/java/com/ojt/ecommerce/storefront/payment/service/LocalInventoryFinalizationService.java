package com.ojt.ecommerce.storefront.payment.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.catalog.repository.G5InventoryRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.inventory.repository.MockInventoryTransactionRepository;
import com.ojt.ecommerce.storefront.payment.port.InventoryFinalizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocalInventoryFinalizationService implements InventoryFinalizationPort {

    private final G5OrderItemRepository orderItemRepository;
    private final G5InventoryRepository inventoryRepository;
    private final MockInventoryTransactionRepository inventoryTransactionRepository;

    @Override
    @Transactional
    public void finalizeInventoryForOrder(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
        
        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            Inventory inv = inventoryRepository.findByVariantVariantId(variant.getVariantId())
                    .orElseThrow(() -> new InvalidRequestException("Inventory not found for variant"));
                    
            if (inv.getQuantity() < item.getQty()) {
                throw new InvalidRequestException("Insufficient stock for variant: " + variant.getSku());
            }
            
            int beforeQty = inv.getQuantity();
            int afterQty = beforeQty - item.getQty();
            
            inv.setQuantity(afterQty);
            if (afterQty == 0) {
                inv.setStatus("OUT_OF_STOCK");
            } else if (inv.getReorderLevel() != null && afterQty <= inv.getReorderLevel()) {
                inv.setStatus("LOW_STOCK");
            } else {
                inv.setStatus("NORMAL"); // Assuming IN_STOCK is a valid status based on previous contexts
            }
            inventoryRepository.save(inv);
            
            InventoryTransaction txn = new InventoryTransaction();
            txn.setVariant(variant);
            txn.setTxnType("SALE");
            txn.setQty(item.getQty());
            txn.setBeforeQty(beforeQty);
            txn.setAfterQty(afterQty);
            txn.setReferenceType("ORDER");
            txn.setReferenceId(order.getOrderId());
            txn.setCreatedAt(LocalDateTime.now());
            txn.setRemark("Stock deducted for order " + order.getOrderNo());
            
            inventoryTransactionRepository.save(txn);
        }
    }
}
