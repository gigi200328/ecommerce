package com.ojt.ecommerce.storefront.payment.port;
import com.ojt.ecommerce.entity.Order;
public interface InventoryFinalizationPort {
    void finalizeInventoryForOrder(Order order);
}