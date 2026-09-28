package com.ojt.ecommerce.storefront.checkout.dto;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
@Data @Builder public class DeliveryZoneResponse {
    private Long zoneId;
    private String zoneCode;
    private String zoneName;
    private String city;
    private String township;
    private String regionOrState;
    private BigDecimal shippingFee;
    private Integer estimatedDays;
}