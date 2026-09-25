package com.ojt.ecommerce.storefront.cart.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.cart.dto.*;
import com.ojt.ecommerce.storefront.cart.repository.CartItemRepository;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final G5ProductVariantRepository variantRepository;
    private final G5ProductImageRepository imageRepository;
    private final G5InventoryRepository inventoryRepository;
    private final G5VariantOptionValueRepository optionValueRepository;

    private Cart getActiveCartOrCreate(Long customerId) {
        return cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")
                .orElseGet(() -> {
                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    cart.setStatus("ACTIVE");
                    cart.setVersion(0L);
                    cart.setCreatedAt(LocalDateTime.now());
                    return cartRepository.save(cart);
                });
    }

    private int getAvailableQuantity(Long variantId) {
        return inventoryRepository.findByVariantVariantId(variantId)
                .map(Inventory::getQuantity)
                .orElse(0);
    }

    @Override
    @Transactional
    public AddToCartResponse addItem(Long customerId, AddToCartRequest request) {
        ProductVariant variant = variantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));
        
        if (!"ACTIVE".equals(variant.getStatus().name()) || !"ACTIVE".equals(variant.getProduct().getStatus().name())) {
            throw new InvalidRequestException("Product is not available");
        }
        
        int available = getAvailableQuantity(variant.getVariantId());
        if (available < request.getQuantity()) {
            throw new InvalidRequestException("Not enough stock available");
        }

        Cart cart = getActiveCartOrCreate(customerId);
        
        CartItem cartItem = cartItemRepository.findByCartCartIdAndVariantVariantId(cart.getCartId(), variant.getVariantId())
                .orElse(null);

        if (cartItem != null) {
            int newQty = cartItem.getQuantity() + request.getQuantity();
            if (newQty > available) {
                throw new InvalidRequestException("Combined quantity exceeds stock");
            }
            cartItem.setQuantity(newQty);
            cartItem.setModifiedAt(LocalDateTime.now());
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setVariant(variant);
            cartItem.setQuantity(request.getQuantity());
            cartItem.setCreatedAt(LocalDateTime.now());
        }
        
        cartItem = cartItemRepository.save(cartItem);
        cart.setModifiedAt(LocalDateTime.now());
        cartRepository.save(cart);
        
        return AddToCartResponse.builder()
                .cartItemId(cartItem.getCartItemId())
                .cartId(cart.getCartId())
                .variantId(variant.getVariantId())
                .quantity(cartItem.getQuantity())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerId) {
        Optional<Cart> optionalCart = cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE");
        if (optionalCart.isEmpty()) {
            return CartResponse.builder()
                    .cartId(null)
                    .status("ACTIVE")
                    .totalQuantity(0)
                    .totalAmount(BigDecimal.ZERO)
                    .items(Collections.emptyList())
                    .build();
        }
        
        Cart cart = optionalCart.get();
        List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());
        
        if (cartItems.isEmpty()) {
            return CartResponse.builder()
                    .cartId(cart.getCartId())
                    .status("ACTIVE")
                    .totalQuantity(0)
                    .totalAmount(BigDecimal.ZERO)
                    .items(Collections.emptyList())
                    .build();
        }
        
        List<Long> variantIds = cartItems.stream().map(ci -> ci.getVariant().getVariantId()).collect(Collectors.toList());
        List<Long> productIds = cartItems.stream().map(ci -> ci.getVariant().getProduct().getProductId()).distinct().collect(Collectors.toList());
        
        Map<Long, Integer> inventoryMap = inventoryRepository.findByVariantVariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(i -> i.getVariant().getVariantId(), Inventory::getQuantity, (a, b) -> a));
        
        Map<Long, String> primaryImageMap = imageRepository.findByProductProductIdIn(productIds).stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .collect(Collectors.toMap(img -> img.getProduct().getProductId(), ProductImage::getImageUrl, (a, b) -> a));
                
        Map<Long, List<VariantOptionValue>> optionsMap = optionValueRepository.findByVariantVariantIdIn(variantIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getVariant().getVariantId()));

        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartItemResponse> itemResponses = new ArrayList<>();

        for (CartItem ci : cartItems) {
            ProductVariant v = ci.getVariant();
            Product p = v.getProduct();
            int qty = inventoryMap.getOrDefault(v.getVariantId(), 0);
            boolean isAvailable = "ACTIVE".equals(v.getStatus().name()) && "ACTIVE".equals(p.getStatus().name()) && qty > 0 && ci.getQuantity() <= qty;
            
            BigDecimal effectivePrice =  v.getSellingPrice();
            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            
            Map<String, String> opts = new HashMap<>();
            optionsMap.getOrDefault(v.getVariantId(), Collections.emptyList())
                    .forEach(optVal -> opts.put(optVal.getOption().getVariation().getName(), optVal.getOption().getValue()));
                    
            itemResponses.add(CartItemResponse.builder()
                    .cartItemId(ci.getCartItemId())
                    .variantId(v.getVariantId())
                    .productId(p.getProductId())
                    .productName(p.getProductName())
                    .imageUrl(primaryImageMap.get(p.getProductId()))
                    .sku(v.getSku())
                    .options(opts)
                    .quantity(ci.getQuantity())
                    .effectivePrice(effectivePrice)
                    .subtotal(subtotal)
                    .availableQuantity(qty)
                    .stockStatus(qty > 0 ? "IN_STOCK" : "OUT_OF_STOCK")
                    .available(isAvailable)
                    .build());
                    
            if (isAvailable) {
                totalQuantity += ci.getQuantity();
                totalAmount = totalAmount.add(subtotal);
            }
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .status(cart.getStatus())
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional
    public void updateItemQuantity(Long customerId, Long cartItemId, UpdateCartItemQuantityRequest request) {
        if (request.getQuantity() <= 0) {
            removeItem(customerId, cartItemId);
            return;
        }
        
        CartItem cartItem = cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
                
        ProductVariant variant = cartItem.getVariant();
        if (!"ACTIVE".equals(variant.getStatus().name()) || !"ACTIVE".equals(variant.getProduct().getStatus().name())) {
            throw new InvalidRequestException("Product is no longer available");
        }
        
        int available = getAvailableQuantity(variant.getVariantId());
        if (available < request.getQuantity()) {
            throw new InvalidRequestException("Quantity exceeds available stock");
        }
        
        cartItem.setQuantity(request.getQuantity());
        cartItem.setModifiedAt(LocalDateTime.now());
        cartItemRepository.save(cartItem);
        
        Cart cart = cartItem.getCart();
        cart.setModifiedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void removeItem(Long customerId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByCartItemIdAndCartCustomerCustomerId(cartItemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        cartItemRepository.delete(cartItem);
        
        Cart cart = cartItem.getCart();
        cart.setModifiedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public GuestCartResolveResponse resolveGuestCart(GuestCartResolveRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return GuestCartResolveResponse.builder().totalQuantity(0).totalAmount(BigDecimal.ZERO).items(Collections.emptyList()).build();
        }
        
        List<Long> variantIds = request.getItems().stream().map(GuestCartItem::getVariantId).distinct().collect(Collectors.toList());
        List<ProductVariant> variants = variantRepository.findAllById(variantIds);
        List<Long> productIds = variants.stream().map(v -> v.getProduct().getProductId()).distinct().collect(Collectors.toList());
        
        Map<Long, ProductVariant> variantMap = variants.stream().collect(Collectors.toMap(ProductVariant::getVariantId, v -> v));
        
        Map<Long, Integer> inventoryMap = inventoryRepository.findByVariantVariantIdIn(variantIds).stream()
                .collect(Collectors.toMap(i -> i.getVariant().getVariantId(), Inventory::getQuantity, (a, b) -> a));
                
        Map<Long, String> primaryImageMap = imageRepository.findByProductProductIdIn(productIds).stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .collect(Collectors.toMap(img -> img.getProduct().getProductId(), ProductImage::getImageUrl, (a, b) -> a));
                
        Map<Long, List<VariantOptionValue>> optionsMap = optionValueRepository.findByVariantVariantIdIn(variantIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getVariant().getVariantId()));

        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<GuestCartResolvedItemResponse> itemResponses = new ArrayList<>();

        for (GuestCartItem reqItem : request.getItems()) {
            ProductVariant v = variantMap.get(reqItem.getVariantId());
            if (v == null) {
                itemResponses.add(GuestCartResolvedItemResponse.builder()
                        .variantId(reqItem.getVariantId())
                        .available(false)
                        .quantity(reqItem.getQuantity())
                        .build());
                continue;
            }
            
            Product p = v.getProduct();
            int qty = inventoryMap.getOrDefault(v.getVariantId(), 0);
            boolean isAvailable = "ACTIVE".equals(v.getStatus().name()) && "ACTIVE".equals(p.getStatus().name()) && qty > 0 && reqItem.getQuantity() <= qty;
            
            BigDecimal effectivePrice = v.getSellingPrice();
            BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(reqItem.getQuantity()));
            
            Map<String, String> opts = new HashMap<>();
            optionsMap.getOrDefault(v.getVariantId(), Collections.emptyList())
                    .forEach(optVal -> opts.put(optVal.getOption().getVariation().getName(), optVal.getOption().getValue()));
                    
            itemResponses.add(GuestCartResolvedItemResponse.builder()
                    .variantId(v.getVariantId())
                    .productId(p.getProductId())
                    .productName(p.getProductName())
                    .imageUrl(primaryImageMap.get(p.getProductId()))
                    .sku(v.getSku())
                    .options(opts)
                    .quantity(reqItem.getQuantity())
                    .effectivePrice(effectivePrice)
                    .subtotal(subtotal)
                    .availableQuantity(qty)
                    .stockStatus(qty > 0 ? "IN_STOCK" : "OUT_OF_STOCK")
                    .available(isAvailable)
                    .build());
                    
            if (isAvailable) {
                totalQuantity += reqItem.getQuantity();
                totalAmount = totalAmount.add(subtotal);
            }
        }
        
        return GuestCartResolveResponse.builder()
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional
    public CartMergeResponse mergeGuestCart(Long customerId, CartMergeRequest request) {
        List<Long> mergedIds = new ArrayList<>();
        List<CartMergeRejectedItem> rejectedItems = new ArrayList<>();
        
        if (request.getItems() == null || request.getItems().isEmpty()) {
            return CartMergeResponse.builder().mergedVariantIds(mergedIds).rejectedItems(rejectedItems).build();
        }
        
        Cart cart = getActiveCartOrCreate(customerId);
        
        for (GuestCartItem reqItem : request.getItems()) {
            ProductVariant variant = variantRepository.findById(reqItem.getVariantId()).orElse(null);
            if (variant == null || !"ACTIVE".equals(variant.getStatus().name()) || !"ACTIVE".equals(variant.getProduct().getStatus().name())) {
                rejectedItems.add(CartMergeRejectedItem.builder()
                        .variantId(reqItem.getVariantId())
                        .reason("Product or variant not available")
                        .build());
                continue;
            }
            
            int available = getAvailableQuantity(variant.getVariantId());
            if (available <= 0) {
                rejectedItems.add(CartMergeRejectedItem.builder()
                        .variantId(reqItem.getVariantId())
                        .reason("Out of stock")
                        .build());
                continue;
            }
            
            CartItem cartItem = cartItemRepository.findByCartCartIdAndVariantVariantId(cart.getCartId(), variant.getVariantId())
                    .orElse(null);
                    
            if (cartItem != null) {
                int newQty = cartItem.getQuantity() + reqItem.getQuantity();
                if (newQty > available) {
                    cartItem.setQuantity(available);
                    rejectedItems.add(CartMergeRejectedItem.builder()
                            .variantId(reqItem.getVariantId())
                            .reason("Combined quantity exceeded stock, clamped to available")
                            .build());
                } else {
                    cartItem.setQuantity(newQty);
                }
                cartItem.setModifiedAt(LocalDateTime.now());
                cartItemRepository.save(cartItem);
                mergedIds.add(variant.getVariantId());
            } else {
                int newQty = reqItem.getQuantity();
                if (newQty > available) {
                    newQty = available;
                    rejectedItems.add(CartMergeRejectedItem.builder()
                            .variantId(reqItem.getVariantId())
                            .reason("Requested quantity exceeded stock, clamped to available")
                            .build());
                }
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setVariant(variant);
                newItem.setQuantity(newQty);
                newItem.setCreatedAt(LocalDateTime.now());
                cartItemRepository.save(newItem);
                mergedIds.add(variant.getVariantId());
            }
        }
        
        cart.setModifiedAt(LocalDateTime.now());
        cartRepository.save(cart);
        
        return CartMergeResponse.builder()
                .mergedVariantIds(mergedIds)
                .rejectedItems(rejectedItems)
                .build();
    }
}
