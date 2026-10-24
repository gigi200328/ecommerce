package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ojt.ecommerce.backofficeinventory.dto.InventoryCreateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.InventoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.InventoryTransactionResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.RestockRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.StockAdjustmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.WriteOffRequestDto;
import com.ojt.ecommerce.backofficeinventory.mapper.InventoryMapper;
import com.ojt.ecommerce.backofficeinventory.mapper.InventoryTransactionMapper;
import com.ojt.ecommerce.backofficeinventory.repository.InventoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.InventoryTransactionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderItemRepository;
import com.ojt.ecommerce.backofficeinventory.repository.OrderRepository;
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.InventoryTransaction;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;

    @Override
    @Transactional
    public List<InventoryResponseDto> getAllInventory() {
        List<ProductVariant> allVariants = productVariantRepository.findAll();
        for (ProductVariant variant : allVariants) {
            if (!inventoryRepository.existsByVariant_VariantId(variant.getVariantId())) {
                createDefaultInventory(variant);
            }
        }

        return inventoryRepository.findAll(Sort.by(Sort.Direction.DESC, "inventoryId")).stream()
                .map(inventoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponseDto getInventoryByVariantId(Long variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with id: " + variantId));

        Inventory inventory = inventoryRepository.findByVariant_VariantId(variantId)
                .orElseGet(() -> createDefaultInventory(variant));

        return inventoryMapper.toResponseDto(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponseDto getInventoryById(Long inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found with id: " + inventoryId));
        return inventoryMapper.toResponseDto(inventory);
    }

    @Override
    @Transactional
    public List<InventoryResponseDto> getLowStockAlerts() {
        getAllInventory();
        return inventoryRepository.findLowStockInventories().stream()
                .map(inventoryMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponseDto initializeInventory(InventoryCreateRequestDto request) {
        Long variantId = request.getVariantId();

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with id: " + variantId));

        int initialQty = request.getInitialQuantity() != null ? request.getInitialQuantity() : 0;
        int reorderLevel = request.getReorderLevel() != null ? request.getReorderLevel() : 10;
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        if (inventoryRepository.existsByVariant_VariantId(variantId)) {
            throw new IllegalArgumentException("Inventory tracking is already initialized for variant id: " + variantId);
        }

        Boolean isPerishable = request.getIsPerishable();
        if (isPerishable == null && variant.getProduct() != null) {
            isPerishable = variant.getProduct().getIsPerishable();
        }

        Inventory inventory = Inventory.builder()
                .variant(variant)
                .quantity(initialQty)
                .reservedQuantity(0)
                .reorderLevel(reorderLevel)
                .status(calculateStatus(initialQty, reorderLevel))
                .isPerishable(Boolean.TRUE.equals(isPerishable))
                .expiryDate(request.getExpiryDate())
                .batchNumber(request.getBatchNumber())
                .createdBy(currentUser)
                .createdAt(now)
                .modifiedBy(currentUser)
                .modifiedAt(now)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryTransaction txn = InventoryTransaction.builder()
                .variant(variant)
                .txnType("RESTOCK")
                .qty(initialQty)
                .beforeQty(0)
                .afterQty(initialQty)
                .referenceType("INITIAL_SETUP")
                .remark("Initial inventory setup")
                .createdBy(currentUser)
                .createdAt(now)
                .build();
        inventoryTransactionRepository.save(txn);

        return inventoryMapper.toResponseDto(savedInventory);
    }

    @Override
    @Transactional
    public InventoryResponseDto restock(RestockRequestDto request) {
        Long variantId = request.getVariantId();
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with id: " + variantId));

        Inventory inventory = inventoryRepository.findByVariant_VariantId(variantId)
                .orElseGet(() -> createDefaultInventory(variant));

        int beforeQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int restockQty = request.getQuantity();
        int afterQty = beforeQty + restockQty;

        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        inventory.setQuantity(afterQty);
        inventory.setStatus(calculateStatus(afterQty, inventory.getReorderLevel()));
        if (request.getExpiryDate() != null) {
            inventory.setExpiryDate(request.getExpiryDate());
            inventory.setIsPerishable(true);
        }
        if (request.getBatchNumber() != null && !request.getBatchNumber().trim().isEmpty()) {
            inventory.setBatchNumber(request.getBatchNumber().trim());
        }
        inventory.setModifiedBy(currentUser);
        inventory.setModifiedAt(now);

        Inventory updatedInventory = inventoryRepository.save(inventory);

        InventoryTransaction txn = InventoryTransaction.builder()
                .variant(variant)
                .txnType("RESTOCK")
                .qty(restockQty)
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .referenceType(request.getReferenceType() != null ? request.getReferenceType() : "MANUAL")
                .referenceId(request.getReferenceId())
                .remark(request.getRemark() != null ? request.getRemark() : "Restocked inventory")
                .createdBy(currentUser)
                .createdAt(now)
                .build();

        inventoryTransactionRepository.save(txn);

        return inventoryMapper.toResponseDto(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponseDto writeOff(WriteOffRequestDto request) {
        Long variantId = request.getVariantId();
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with id: " + variantId));

        Inventory inventory = inventoryRepository.findByVariant_VariantId(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for variant id: " + variantId));

        int beforeQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int reservedQty = inventory.getReservedQuantity() != null ? inventory.getReservedQuantity() : 0;
        int availableQty = Math.max(0, beforeQty - reservedQty);
        int writeOffQty = request.getQuantity();

        if (writeOffQty <= 0) {
            throw new IllegalArgumentException("Write-off quantity must be greater than 0.");
        }
        if (writeOffQty > beforeQty) {
            throw new IllegalArgumentException("Cannot write off " + writeOffQty + " units. Current stock is only " + beforeQty + " units.");
        }
        if (writeOffQty > availableQty) {
            throw new IllegalArgumentException("Cannot write off " + writeOffQty + " units. Available stock is only " + availableQty + " units (" + reservedQty + " reserved for customer orders).");
        }

        int afterQty = beforeQty - writeOffQty;

        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        inventory.setQuantity(afterQty);
        inventory.setStatus(calculateStatus(afterQty, inventory.getReorderLevel()));
        inventory.setModifiedBy(currentUser);
        inventory.setModifiedAt(now);

        Inventory updatedInventory = inventoryRepository.save(inventory);

        InventoryTransaction txn = InventoryTransaction.builder()
                .variant(variant)
                .txnType("WRITE_OFF")
                .qty(-writeOffQty)
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .referenceType("WRITE_OFF")
                .remark(request.getRemark())
                .createdBy(currentUser)
                .createdAt(now)
                .build();

        inventoryTransactionRepository.save(txn);

        return inventoryMapper.toResponseDto(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponseDto adjustStock(StockAdjustmentRequestDto request) {
        Long variantId = request.getVariantId();
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new EntityNotFoundException("Product variant not found with id: " + variantId));

        Inventory inventory = inventoryRepository.findByVariant_VariantId(variantId)
                .orElseGet(() -> createDefaultInventory(variant));

        int beforeQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int newQty = request.getNewQuantity();
        int delta = newQty - beforeQty;

        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        inventory.setQuantity(newQty);
        inventory.setStatus(calculateStatus(newQty, inventory.getReorderLevel()));
        inventory.setModifiedBy(currentUser);
        inventory.setModifiedAt(now);

        Inventory updatedInventory = inventoryRepository.save(inventory);

        InventoryTransaction txn = InventoryTransaction.builder()
                .variant(variant)
                .txnType("ADJUSTMENT")
                .qty(delta)
                .beforeQty(beforeQty)
                .afterQty(newQty)
                .referenceType("PHYSICAL_COUNT")
                .remark(request.getRemark())
                .createdBy(currentUser)
                .createdAt(now)
                .build();

        inventoryTransactionRepository.save(txn);

        return inventoryMapper.toResponseDto(updatedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransactionResponseDto> getTransactionHistory(Long variantId, String txnType) {
        List<InventoryTransaction> transactions;

        if (variantId != null) {
            transactions = inventoryTransactionRepository.findByVariant_VariantIdOrderByCreatedAtDesc(variantId);
        } else if (txnType != null && !txnType.trim().isEmpty()) {
            transactions = inventoryTransactionRepository.findByTxnTypeOrderByCreatedAtDesc(txnType.toUpperCase());
        } else {
            transactions = inventoryTransactionRepository.findAllByOrderByCreatedAtDesc();
        }

        return transactions.stream()
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deductStockForOrder(Long orderId) {
        if (orderId == null) {
            return;
        }

        // Idempotency check: Do not deduct if already deducted for this order
        boolean alreadyDeducted = inventoryTransactionRepository
                .existsByTxnTypeAndReferenceTypeAndReferenceId("SALE", "ORDER", orderId);
        if (alreadyDeducted) {
            log.info("Stock already deducted for order #{}. Skipping.", orderId);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Cannot deduct stock: Order #{} not found.", orderId);
            return;
        }

        List<OrderItem> items = orderItemRepository.findByOrder_OrderId(orderId);
        if (items == null || items.isEmpty()) {
            items = order.getOrderItems();
        }

        if (items == null || items.isEmpty()) {
            log.warn("Order #{} has no items to deduct stock for.", orderId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        User currentUser = getCurrentUser();

        for (OrderItem item : items) {
            ProductVariant variant = item.getVariant();
            if (variant == null) {
                continue;
            }

            Long variantId = variant.getVariantId();
            Inventory inventory = inventoryRepository.findByVariant_VariantId(variantId)
                    .orElseGet(() -> createDefaultInventory(variant));

            int beforeQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
            int deductQty = item.getQty() != null ? item.getQty() : 1;
            int afterQty = Math.max(0, beforeQty - deductQty);

            inventory.setQuantity(afterQty);
            inventory.setStatus(calculateStatus(afterQty, inventory.getReorderLevel()));
            inventory.setModifiedAt(now);
            if (currentUser != null) {
                inventory.setModifiedBy(currentUser);
            }
            inventoryRepository.save(inventory);

            // Record SALE transaction audit
            InventoryTransaction txn = InventoryTransaction.builder()
                    .variant(variant)
                    .txnType("SALE")
                    .qty(-deductQty)
                    .beforeQty(beforeQty)
                    .afterQty(afterQty)
                    .referenceType("ORDER")
                    .referenceId(orderId)
                    .remark("Customer Purchase - Order #" + order.getOrderNo() + " (Qty: " + deductQty + ")")
                    .createdBy(currentUser)
                    .createdAt(now)
                    .build();

            inventoryTransactionRepository.save(txn);
            log.info("Deducted {} units of variant #{} for order #{}. New stock: {}", deductQty, variantId, orderId, afterQty);
        }
    }

    @Override
    @Transactional
    public void restoreStockForRefund(Long variantId, int quantity, Long refundId, String remark) {
        if (variantId == null || quantity <= 0) {
            return;
        }

        ProductVariant variant = productVariantRepository.findById(variantId).orElse(null);
        if (variant == null) {
            log.warn("Cannot restore stock for refund #{}: Variant #{} not found.", refundId, variantId);
            return;
        }

        Inventory inventory = inventoryRepository.findByVariant_VariantId(variantId)
                .orElseGet(() -> createDefaultInventory(variant));

        int beforeQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int afterQty = beforeQty + quantity;
        LocalDateTime now = LocalDateTime.now();
        User currentUser = getCurrentUser();

        inventory.setQuantity(afterQty);
        inventory.setStatus(calculateStatus(afterQty, inventory.getReorderLevel()));
        inventory.setModifiedAt(now);
        if (currentUser != null) {
            inventory.setModifiedBy(currentUser);
        }
        inventoryRepository.save(inventory);

        // Record RETURN transaction audit
        InventoryTransaction txn = InventoryTransaction.builder()
                .variant(variant)
                .txnType("RETURN")
                .qty(quantity)
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .referenceType("REFUND")
                .referenceId(refundId)
                .remark(remark != null && !remark.isBlank() ? remark : ("Approved refund #" + refundId + " returned to stock"))
                .createdBy(currentUser)
                .createdAt(now)
                .build();

        inventoryTransactionRepository.save(txn);
        log.info("Restocked {} units of variant #{} from refund #{}. New stock: {}", quantity, variantId, refundId, afterQty);
    }

    private Inventory createDefaultInventory(ProductVariant variant) {
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        int defaultQty = 0;
        Boolean isPerishable = variant.getProduct() != null && Boolean.TRUE.equals(variant.getProduct().getIsPerishable());
        Inventory newInventory = Inventory.builder()
                .variant(variant)
                .quantity(defaultQty)
                .reservedQuantity(0)
                .reorderLevel(10)
                .status("OUT_OF_STOCK")
                .isPerishable(isPerishable)
                .createdBy(currentUser)
                .createdAt(now)
                .modifiedBy(currentUser)
                .modifiedAt(now)
                .build();

        return inventoryRepository.save(newInventory);
    }

    private String calculateStatus(Integer quantity, Integer reorderLevel) {
        if (quantity == null || quantity <= 0) {
            return "OUT_OF_STOCK";
        } else if (reorderLevel != null && quantity <= reorderLevel) {
            return "LOW_STOCK";
        } else {
            return "NORMAL";
        }
    }

    private User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                return ((CustomUserDetails) authentication.getPrincipal()).getUser();
            }
        } catch (Exception ignored) {
        }
        return userRepository.findByEmail("admin@ecommerce.com").orElse(null);
    }
}
