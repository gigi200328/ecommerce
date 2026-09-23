package com.ojt.ecommerce.backofficeinventory.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
import com.ojt.ecommerce.backofficeinventory.repository.ProductVariantRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.InventoryTransaction;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final InventoryMapper inventoryMapper;
    private final InventoryTransactionMapper transactionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getAllInventory() {
        return inventoryRepository.findAll().stream()
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
    @Transactional(readOnly = true)
    public List<InventoryResponseDto> getLowStockAlerts() {
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

        if (inventoryRepository.existsByVariant_VariantId(variantId)) {
            throw new IllegalArgumentException("Inventory tracking is already initialized for variant id: " + variantId);
        }

        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        int initialQty = request.getInitialQuantity() != null ? request.getInitialQuantity() : 0;
        int reorderLevel = request.getReorderLevel() != null ? request.getReorderLevel() : 10;

        Inventory inventory = Inventory.builder()
                .variant(variant)
                .quantity(initialQty)
                .reservedQuantity(0)
                .reorderLevel(reorderLevel)
                .status(calculateStatus(initialQty, reorderLevel))
                .createdBy(currentUser)
                .createdAt(now)
                .modifiedBy(currentUser)
                .modifiedAt(now)
                .build();

        Inventory savedInventory = inventoryRepository.save(inventory);

        if (initialQty > 0) {
            InventoryTransaction txn = InventoryTransaction.builder()
                    .variant(variant)
                    .txnType("ADJUSTMENT")
                    .qty(initialQty)
                    .beforeQty(0)
                    .afterQty(initialQty)
                    .referenceType("INITIAL")
                    .remark("Initial inventory setup")
                    .createdBy(currentUser)
                    .createdAt(now)
                    .build();
            inventoryTransactionRepository.save(txn);
        }

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
        int writeOffQty = request.getQuantity();

        if (writeOffQty > beforeQty) {
            throw new IllegalArgumentException("Cannot write off " + writeOffQty + " units. Current stock is only " + beforeQty + " units.");
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

    private Inventory createDefaultInventory(ProductVariant variant) {
        User currentUser = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        Inventory newInventory = Inventory.builder()
                .variant(variant)
                .quantity(0)
                .reservedQuantity(0)
                .reorderLevel(10)
                .status("OUT_OF_STOCK")
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
