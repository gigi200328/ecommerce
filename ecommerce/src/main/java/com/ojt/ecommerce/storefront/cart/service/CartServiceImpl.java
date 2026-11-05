package com.ojt.ecommerce.storefront.cart.service;

import com.ojt.ecommerce.entity.*;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderRepository;
import com.ojt.ecommerce.storefront.exception.AccessDeniedException;
import com.ojt.ecommerce.storefront.exception.ConflictException;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.reservation.service.InventoryReservationService;
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
    private final G5ProductVariantImageRepository variantImageRepository;
    private final G5InventoryRepository inventoryRepository;
    private final G5VariantOptionValueRepository optionValueRepository;
    private final G5OrderRepository orderRepository;
    private final InventoryReservationService reservationService;

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
        return reservationService.getAvailableStock(variantId);
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
        
        Map<Long, Integer> inventoryMap = reservationService.getAvailableStockForVariants(variantIds);
        
        List<ProductVariantImage> variantImages = (variantImageRepository != null && !variantIds.isEmpty())
                ? variantImageRepository.findByProductVariantVariantIdIn(variantIds)
                : Collections.emptyList();
        Map<Long, List<ProductVariantImage>> variantImagesMap = variantImages.stream()
                .filter(img -> img.getProductVariant() != null && img.getProductVariant().getVariantId() != null)
                .sorted(Comparator.comparing(ProductVariantImage::getProductVariantImageId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(img -> img.getProductVariant().getVariantId()));

        List<ProductImage> productImages = (imageRepository != null && !productIds.isEmpty())
                ? imageRepository.findByProductProductIdIn(productIds)
                : Collections.emptyList();
        Map<Long, List<ProductImage>> productImagesMap = productImages.stream()
                .filter(img -> img.getProduct() != null && img.getProduct().getProductId() != null)
                .sorted(Comparator.comparing(ProductImage::getImageId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(img -> img.getProduct().getProductId()));
                
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
                    .imageUrl(resolveVariantImageUrl(v.getVariantId(), p.getProductId(), variantImagesMap, productImagesMap))
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
        
        Map<Long, Integer> inventoryMap = reservationService.getAvailableStockForVariants(variantIds);
                
        List<ProductVariantImage> variantImages = (variantImageRepository != null && !variantIds.isEmpty())
                ? variantImageRepository.findByProductVariantVariantIdIn(variantIds)
                : Collections.emptyList();
        Map<Long, List<ProductVariantImage>> variantImagesMap = variantImages.stream()
                .filter(img -> img.getProductVariant() != null && img.getProductVariant().getVariantId() != null)
                .sorted(Comparator.comparing(ProductVariantImage::getProductVariantImageId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(img -> img.getProductVariant().getVariantId()));

        List<ProductImage> productImages = (imageRepository != null && !productIds.isEmpty())
                ? imageRepository.findByProductProductIdIn(productIds)
                : Collections.emptyList();
        Map<Long, List<ProductImage>> productImagesMap = productImages.stream()
                .filter(img -> img.getProduct() != null && img.getProduct().getProductId() != null)
                .sorted(Comparator.comparing(ProductImage::getImageId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(img -> img.getProduct().getProductId()));
                
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
                    .imageUrl(resolveVariantImageUrl(v.getVariantId(), p.getProductId(), variantImagesMap, productImagesMap))
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

    private String resolveVariantImageUrl(
            Long variantId,
            Long productId,
            Map<Long, List<ProductVariantImage>> variantImagesMap,
            Map<Long, List<ProductImage>> productImagesMap) {

        if (variantId != null) {
            List<ProductVariantImage> varImgs = variantImagesMap.getOrDefault(variantId, Collections.emptyList());

            // 1. Selected variant primary image
            for (ProductVariantImage img : varImgs) {
                if (Boolean.TRUE.equals(img.getIsPrimary()) && img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    return img.getImageUrl().trim();
                }
            }

            // 2. Selected variant first available image
            for (ProductVariantImage img : varImgs) {
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    return img.getImageUrl().trim();
                }
            }
        }

        if (productId != null) {
            List<ProductImage> prodImgs = productImagesMap.getOrDefault(productId, Collections.emptyList());

            // 3. Product primary image
            for (ProductImage img : prodImgs) {
                if (Boolean.TRUE.equals(img.getIsPrimary()) && img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    return img.getImageUrl().trim();
                }
            }

            // 4. Product first available image
            for (ProductImage img : prodImgs) {
                if (img.getImageUrl() != null && !img.getImageUrl().trim().isEmpty()) {
                    return img.getImageUrl().trim();
                }
            }
        }

        // 5. Null if no images exist
        return null;
    }

    @Override
    @Transactional
    public RestoreToCartResponse restoreFailedCartOrder(Long customerId, Long orderId) {
        // 1. Fetch order with pessimistic write lock for concurrency protection
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // 2. Ownership check: customer must own the order
        if (order.getCustomer() == null || !order.getCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You do not have permission to restore this order");
        }

        // 3. Checkout type check: must be CART
        if (!"CART".equalsIgnoreCase(order.getCheckoutType())) {
            throw new InvalidRequestException("Only CART orders can be restored to active cart");
        }

        // 4. Linked cart check
        Cart cartA = order.getCart();
        if (cartA == null) {
            throw new InvalidRequestException("Order does not have a linked cart to restore");
        }

        // 5. Payment / Order terminal failure check
        // Check if SUCCESS or PAID
        if ("PAID".equalsIgnoreCase(order.getOrderStatus()) || "SUCCESS".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ConflictException("Cannot restore an order that is already paid or successful");
        }

        // Check if PENDING (payment in progress, not terminal failure)
        if ("PENDING".equalsIgnoreCase(order.getPaymentStatus()) && !"CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new InvalidRequestException("Cannot restore an order with payment still pending");
        }

        // Must be in a terminal failed/cancelled state
        boolean isFailedOrCancelled = "FAILED".equalsIgnoreCase(order.getPaymentStatus())
                || "CANCELLED".equalsIgnoreCase(order.getOrderStatus());
        if (!isFailedOrCancelled) {
            throw new InvalidRequestException("Order is not in a failed or cancelled payment state");
        }

        // Guard: Disallow restoring expired orders
        if (reservationService.isOrderExpired(order.getOrderId())) {
            throw new InvalidRequestException("Cannot restore items from an expired order");
        }

        // 6. Idempotency / Already restored check
        if ("RESTORED".equalsIgnoreCase(cartA.getStatus())) {
            throw new ConflictException("This cart order has already been restored");
        }
        if ("CHECKED_OUT".equalsIgnoreCase(cartA.getStatus())) {
            throw new ConflictException("This cart order was already completed and checked out");
        }

        // 7. Load original items from Cart A (with fallback to Order items if cart items were cleared)
        List<CartItem> cartAItems = cartItemRepository.findByCartCartId(cartA.getCartId());

        class SourceItem {
            final ProductVariant variant;
            final int quantity;
            final String productName;

            SourceItem(ProductVariant variant, int quantity, String productName) {
                this.variant = variant;
                this.quantity = quantity;
                this.productName = productName;
            }
        }

        List<SourceItem> sourceItems = new ArrayList<>();
        if (!cartAItems.isEmpty()) {
            for (CartItem ci : cartAItems) {
                String pName = (ci.getVariant() != null && ci.getVariant().getProduct() != null)
                        ? ci.getVariant().getProduct().getProductName()
                        : "Unknown Product";
                sourceItems.add(new SourceItem(ci.getVariant(), ci.getQuantity(), pName));
            }
        } else if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            for (OrderItem oi : order.getOrderItems()) {
                String pName = oi.getProductName() != null
                        ? oi.getProductName()
                        : (oi.getVariant() != null && oi.getVariant().getProduct() != null
                                ? oi.getVariant().getProduct().getProductName()
                                : "Unknown Product");
                sourceItems.add(new SourceItem(oi.getVariant(), oi.getQty(), pName));
            }
        }

        if (sourceItems.isEmpty()) {
            throw new InvalidRequestException("No items found to restore from this order");
        }

        // 8. Resolve or lazily create current ACTIVE Cart B
        // Note: Cart A is NEVER reactivated as ACTIVE. Cart B is the only active cart.
        Cart cartB = getActiveCartOrCreate(customerId);

        // Sanity check: Cart B must not be Cart A
        if (cartB.getCartId().equals(cartA.getCartId())) {
            throw new ConflictException("Cart conflict: active cart cannot be the original checkout cart");
        }

        // 9. Process each item: validate variant, check stock, clamp/merge into Cart B
        List<RestoreToCartResponse.RestoredItemDetail> itemDetails = new ArrayList<>();
        int restoredCount = 0;
        int skippedCount = 0;

        for (SourceItem item : sourceItems) {
            ProductVariant variant = item.variant;
            int requestedQty = item.quantity;
            String prodName = item.productName;

            if (variant == null) {
                itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                        .variantId(null)
                        .productName(prodName)
                        .requestedQuantity(requestedQty)
                        .restoredQuantity(0)
                        .status("UNAVAILABLE")
                        .message("Variant reference no longer exists")
                        .build());
                skippedCount++;
                continue;
            }

            // Fresh check from database for variant and product active status
            ProductVariant currentVariant = variantRepository.findById(variant.getVariantId()).orElse(null);
            boolean isVariantAvailable = currentVariant != null
                    && currentVariant.getStatus() != null
                    && "ACTIVE".equalsIgnoreCase(currentVariant.getStatus().name())
                    && currentVariant.getProduct() != null
                    && currentVariant.getProduct().getStatus() != null
                    && "ACTIVE".equalsIgnoreCase(currentVariant.getProduct().getStatus().name());

            if (!isVariantAvailable) {
                itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                        .variantId(variant.getVariantId())
                        .productName(prodName)
                        .requestedQuantity(requestedQty)
                        .restoredQuantity(0)
                        .status("UNAVAILABLE")
                        .message("Product or variant is no longer available")
                        .build());
                skippedCount++;
                continue;
            }

            int availableStock = getAvailableQuantity(currentVariant.getVariantId());
            if (availableStock <= 0) {
                itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                        .variantId(currentVariant.getVariantId())
                        .productName(prodName)
                        .requestedQuantity(requestedQty)
                        .restoredQuantity(0)
                        .status("OUT_OF_STOCK")
                        .message("Item is out of stock")
                        .build());
                skippedCount++;
                continue;
            }

            // Check if Cart B already contains this variant
            Optional<CartItem> existingItemOpt = cartItemRepository.findByCartCartIdAndVariantVariantId(
                    cartB.getCartId(), currentVariant.getVariantId());

            if (existingItemOpt.isPresent()) {
                CartItem existingItem = existingItemOpt.get();
                int currentQtyInCart = existingItem.getQuantity();
                int combinedQty = currentQtyInCart + requestedQty;

                if (combinedQty > availableStock) {
                    if (currentQtyInCart >= availableStock) {
                        itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                                .variantId(currentVariant.getVariantId())
                                .productName(prodName)
                                .requestedQuantity(requestedQty)
                                .restoredQuantity(0)
                                .status("SKIPPED")
                                .message("Cart already has maximum available stock (" + availableStock + ")")
                                .build());
                        skippedCount++;
                    } else {
                        int addedQty = availableStock - currentQtyInCart;
                        existingItem.setQuantity(availableStock);
                        existingItem.setModifiedAt(LocalDateTime.now());
                        cartItemRepository.save(existingItem);
                        itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                                .variantId(currentVariant.getVariantId())
                                .productName(prodName)
                                .requestedQuantity(requestedQty)
                                .restoredQuantity(addedQty)
                                .status("CLAMPED")
                                .message("Quantity clamped to available stock (added " + addedQty + ", total " + availableStock + ")")
                                .build());
                        restoredCount++;
                    }
                } else {
                    existingItem.setQuantity(combinedQty);
                    existingItem.setModifiedAt(LocalDateTime.now());
                    cartItemRepository.save(existingItem);
                    itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                            .variantId(currentVariant.getVariantId())
                            .productName(prodName)
                            .requestedQuantity(requestedQty)
                            .restoredQuantity(requestedQty)
                            .status("RESTORED")
                            .message("Merged into existing cart item")
                            .build());
                    restoredCount++;
                }
            } else {
                int addQty = Math.min(requestedQty, availableStock);
                CartItem newItem = new CartItem();
                newItem.setCart(cartB);
                newItem.setVariant(currentVariant);
                newItem.setQuantity(addQty);
                newItem.setCreatedAt(LocalDateTime.now());
                cartItemRepository.save(newItem);

                if (addQty < requestedQty) {
                    itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                            .variantId(currentVariant.getVariantId())
                            .productName(prodName)
                            .requestedQuantity(requestedQty)
                            .restoredQuantity(addQty)
                            .status("CLAMPED")
                            .message("Added " + addQty + " (clamped due to available stock of " + availableStock + ")")
                            .build());
                } else {
                    itemDetails.add(RestoreToCartResponse.RestoredItemDetail.builder()
                            .variantId(currentVariant.getVariantId())
                            .productName(prodName)
                            .requestedQuantity(requestedQty)
                            .restoredQuantity(addQty)
                            .status("RESTORED")
                            .message("Added to cart")
                            .build());
                }
                restoredCount++;
            }
        }

        // 10. If 0 items could be restored because everything was unavailable or out of stock:
        if (restoredCount == 0 && skippedCount > 0) {
            throw new InvalidRequestException("None of the items from this order could be restored (out of stock or unavailable)");
        }

        // 11. Update Cart B timestamp
        cartB.setModifiedAt(LocalDateTime.now());
        cartRepository.save(cartB);

        // 12. Mark Cart A as RESTORED (persistent idempotency)
        cartA.setStatus("RESTORED");
        cartA.setModifiedAt(LocalDateTime.now());
        cartRepository.save(cartA);

        return RestoreToCartResponse.builder()
                .orderId(order.getOrderId())
                .activeCartId(cartB.getCartId())
                .restoredItemCount(restoredCount)
                .skippedItemCount(skippedCount)
                .message("Successfully restored " + restoredCount + " item(s) to active cart")
                .items(itemDetails)
                .build();
    }
}
