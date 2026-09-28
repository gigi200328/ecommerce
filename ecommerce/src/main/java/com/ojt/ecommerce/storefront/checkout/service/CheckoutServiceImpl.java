package com.ojt.ecommerce.storefront.checkout.service;



import com.ojt.ecommerce.entity.*;

import com.ojt.ecommerce.storefront.exception.*;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.cart.repository.CartItemRepository;
import com.ojt.ecommerce.storefront.cart.repository.CartRepository;
import com.ojt.ecommerce.storefront.catalog.repository.*;

import com.ojt.ecommerce.storefront.checkout.dto.*;
import com.ojt.ecommerce.storefront.checkout.repository.*;
import com.ojt.ecommerce.storefront.profile.repository.CustomerAddressRepository;
import com.ojt.ecommerce.storefront.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final G5OrderRepository orderRepository;
    private final G5OrderItemRepository orderItemRepository;
    private final G5OrderAddressRepository orderAddressRepository;
    private final G5OrderStatusHistoryRepository orderStatusHistoryRepository;
    
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final G5InventoryRepository inventoryRepository;
    private final G5VariantOptionValueRepository optionValueRepository;
    
    private final ShippingService shippingService;
    private String buildJson(java.util.Map<String, String> map) { StringBuilder sb = new StringBuilder("{"); boolean first = true; for (java.util.Map.Entry<String, String> e : map.entrySet()) { if (!first) sb.append(","); sb.append("\"").append(e.getKey()).append("\":\"").append(e.getValue()).append("\""); first = false; } sb.append("}"); return sb.toString(); }

    private String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + dateStr + "-" + randomStr;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long customerId, CreateOrderRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Cart cart = cartRepository.findByCustomerCustomerIdAndStatus(customerId, "ACTIVE")
                .orElseThrow(() -> new InvalidRequestException("Empty cart"));
                
        List<CartItem> cartItems = cartItemRepository.findByCartCartId(cart.getCartId());
        if (cartItems.isEmpty()) {
            throw new InvalidRequestException("Empty cart");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (CartItem ci : cartItems) {
            ProductVariant v = ci.getVariant();
            Product p = v.getProduct();
            if (!"ACTIVE".equals(v.getStatus().name()) || !"ACTIVE".equals(p.getStatus().name())) {
                throw new InvalidRequestException("Product or variant is no longer available: " + p.getProductName());
            }
            
            Inventory inv = inventoryRepository.findByVariantVariantId(v.getVariantId()).orElse(null);
            int available = inv != null ? inv.getQuantity() : 0;
            
            if (available < ci.getQuantity()) {
                throw new InvalidRequestException("Insufficient stock for product: " + p.getProductName());
            }
            
            BigDecimal unitPrice = v.getSellingPrice();
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        OrderAddress address = new OrderAddress();
        ShippingQuoteResponse quote;

        if (request.getSavedAddressId() != null) {
            CustomerAddress ca = customerAddressRepository.findByAddressIdAndCustomerCustomerId(request.getSavedAddressId(), customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Saved address not found"));
            address.setRecipientName(ca.getRecipientName());
            address.setPhoneNumber(ca.getPhoneNumber());
            address.setAddressLine1(ca.getAddressLine1());
            address.setAddressLine2(ca.getAddressLine2());
            address.setTownship(ca.getTownship());
            address.setCity(ca.getCity());
            address.setRegionOrState(ca.getRegionOrState());
            
            quote = shippingService.getShippingQuote(customerId, request.getSavedAddressId());
        } else if (request.getShippingAddress() != null) {
            CustomShippingAddressRequest csa = request.getShippingAddress();
            address.setRecipientName(csa.getRecipientName());
            address.setPhoneNumber(csa.getPhoneNumber());
            address.setAddressLine1(csa.getAddressLine1());
            address.setAddressLine2(csa.getAddressLine2());
            address.setTownship(csa.getTownship());
            address.setCity(csa.getCity());
            address.setRegionOrState(csa.getRegionOrState());
            
            CustomShippingQuoteRequest csq = new CustomShippingQuoteRequest();
            csq.setCity(csa.getCity());
            csq.setTownship(csa.getTownship());
            quote = shippingService.getCustomShippingQuote(csq);
        } else {
            throw new InvalidRequestException("Shipping address is required");
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal shippingFee = quote.getShippingFee();
        BigDecimal total = subtotal.subtract(discount).add(tax).add(shippingFee);

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setCustomer(customer);
        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(discount);
        order.setTaxAmount(tax);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(total);
        order.setOrderStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        
        order = orderRepository.save(order);

        address.setOrder(order);
        orderAddressRepository.save(address);

        for (CartItem ci : cartItems) {
            ProductVariant v = ci.getVariant();
            BigDecimal unitPrice =  v.getSellingPrice();
            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
            
            List<VariantOptionValue> options = optionValueRepository.findByVariantVariantId(v.getVariantId());
            Map<String, String> optsMap = new HashMap<>();
            for (VariantOptionValue opt : options) {
                optsMap.put(opt.getOption().getVariation().getName(), opt.getOption().getValue());
            }
            
            String variantAttributesJson = null;
            variantAttributesJson = buildJson(optsMap);
            
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariant(v);
            orderItem.setQty(ci.getQuantity());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(itemSubtotal);
            orderItem.setDiscountAmount(BigDecimal.ZERO);
            orderItem.setProductName(v.getProduct().getProductName());
            orderItem.setVariantAttributes(variantAttributesJson);
            
            orderItemRepository.save(orderItem);
        }

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(null);
        history.setNewStatus("PENDING");
        history.setChangedAt(LocalDateTime.now());
        history.setRemark("Order created");
        
        orderStatusHistoryRepository.save(history);

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(order.getTaxAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt().toString())
                .build();
    }
}
