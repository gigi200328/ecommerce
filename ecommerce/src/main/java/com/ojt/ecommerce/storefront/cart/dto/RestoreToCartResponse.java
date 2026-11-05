package com.ojt.ecommerce.storefront.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreToCartResponse {

    private Long orderId;
    private Long activeCartId;
    private int restoredItemCount;
    private int skippedItemCount;
    private String message;
    private List<RestoredItemDetail> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RestoredItemDetail {
        private Long variantId;
        private String productName;
        private Integer requestedQuantity;
        private Integer restoredQuantity;
        private String status;
        private String message;
    }
}
