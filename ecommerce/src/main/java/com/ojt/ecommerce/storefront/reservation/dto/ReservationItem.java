package com.ojt.ecommerce.storefront.reservation.dto;

import com.ojt.ecommerce.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationItem {
    private ProductVariant variant;
    private int quantity;
}
