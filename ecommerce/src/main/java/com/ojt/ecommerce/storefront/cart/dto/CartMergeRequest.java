package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Data;
import java.util.List;
@Data public class CartMergeRequest {
    private List<GuestCartItem> items;
}