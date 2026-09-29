package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data @Builder public class CartMergeResponse {
    private List<Long> mergedVariantIds;
    private List<CartMergeRejectedItem> rejectedItems;
}