package com.ojt.ecommerce.storefront.cart.dto;
import lombok.Data;
import java.util.List;
@Data public class GuestCartResolveRequest {
    private List<GuestCartItem> items;
}