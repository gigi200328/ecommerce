package com.ojt.ecommerce.storefront.order.port;
import com.ojt.ecommerce.storefront.order.dto.ShipmentTracking;
import java.util.List;
public interface ShipmentTrackingPort {
    List<ShipmentTracking> getTrackingByOrderId(Long orderId);
}