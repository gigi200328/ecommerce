package com.ojt.ecommerce.backofficeinventory.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

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
import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.InventoryTransaction;
import com.ojt.ecommerce.entity.Product;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.entity.User;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private InventoryTransactionMapper transactionMapper;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private User mockUser;
    private Product mockProduct;
    private ProductVariant mockVariant;
    private Inventory mockInventory;
    private InventoryResponseDto mockInventoryDto;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email("admin@ecommerce.com")
                .userName("Admin User")
                .build();

        mockProduct = Product.builder()
                .productId(1L)
                .productName("Nike Air Max")
                .build();

        mockVariant = ProductVariant.builder()
                .variantId(1L)
                .sku("NIKE-AIR-BLK-42")
                .sellingPrice(BigDecimal.valueOf(150.00))
                .product(mockProduct)
                .build();

        mockInventory = Inventory.builder()
                .inventoryId(1L)
                .variant(mockVariant)
                .quantity(50)
                .reservedQuantity(5)
                .reorderLevel(10)
                .status("NORMAL")
                .createdAt(LocalDateTime.now())
                .createdBy(mockUser)
                .build();

        mockInventoryDto = InventoryResponseDto.builder()
                .inventoryId(1L)
                .variantId(1L)
                .sku("NIKE-AIR-BLK-42")
                .productName("Nike Air Max")
                .quantity(50)
                .reservedQuantity(5)
                .availableQuantity(45)
                .reorderLevel(10)
                .status("NORMAL")
                .build();
    }

    // =========================================================================
    // 1. GET ALL INVENTORY
    // =========================================================================
    @Test
    @DisplayName("Test 1: getAllInventory should return list sorted descending by inventoryId")
    void testGetAllInventory() {
        // Arrange
        when(inventoryRepository.findAll(any(Sort.class))).thenReturn(List.of(mockInventory));
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        List<InventoryResponseDto> result = inventoryService.getAllInventory();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("NIKE-AIR-BLK-42", result.get(0).getSku());
        assertEquals(50, result.get(0).getQuantity());
        verify(inventoryRepository, times(1)).findAll(any(Sort.class));
    }

    // =========================================================================
    // 2. GET INVENTORY BY VARIANT ID
    // =========================================================================
    @Test
    @DisplayName("Test 2: getInventoryByVariantId returns existing inventory")
    void testGetInventoryByVariantId_Existing() {
        // Arrange
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.of(mockInventory));
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        InventoryResponseDto result = inventoryService.getInventoryByVariantId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getInventoryId());
        assertEquals("NORMAL", result.getStatus());
        verify(inventoryRepository, times(1)).findByVariant_VariantId(1L);
    }

    @Test
    @DisplayName("Test 3: getInventoryByVariantId creates default inventory when not found")
    void testGetInventoryByVariantId_CreateDefault() {
        // Arrange
        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@ecommerce.com")).thenReturn(Optional.of(mockUser));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        InventoryResponseDto result = inventoryService.getInventoryByVariantId(1L);

        // Assert
        assertNotNull(result);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    @DisplayName("Test 4: getInventoryByVariantId throws EntityNotFoundException if variant not found")
    void testGetInventoryByVariantId_VariantNotFound() {
        // Arrange
        when(productVariantRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> inventoryService.getInventoryByVariantId(99L));
    }

    // =========================================================================
    // 3. LOW STOCK ALERTS
    // =========================================================================
    @Test
    @DisplayName("Test 5: getLowStockAlerts should return low stock items")
    void testGetLowStockAlerts() {
        // Arrange
        Inventory lowStockItem = Inventory.builder()
                .inventoryId(2L)
                .variant(mockVariant)
                .quantity(5)
                .reservedQuantity(0)
                .reorderLevel(10)
                .status("LOW_STOCK")
                .build();

        InventoryResponseDto lowStockDto = InventoryResponseDto.builder()
                .inventoryId(2L)
                .quantity(5)
                .status("LOW_STOCK")
                .build();

        when(inventoryRepository.findLowStockInventories()).thenReturn(List.of(lowStockItem));
        when(inventoryMapper.toResponseDto(lowStockItem)).thenReturn(lowStockDto);

        // Act
        List<InventoryResponseDto> result = inventoryService.getLowStockAlerts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("LOW_STOCK", result.get(0).getStatus());
        assertEquals(5, result.get(0).getQuantity());
        verify(inventoryRepository, times(1)).findLowStockInventories();
    }

    // =========================================================================
    // 4. INITIALIZE INVENTORY
    // =========================================================================
    @Test
    @DisplayName("Test 6: initializeInventory creates new record successfully")
    void testInitializeInventory_Success() {
        // Arrange
        InventoryCreateRequestDto request = new InventoryCreateRequestDto();
        request.setVariantId(1L);
        request.setInitialQuantity(30);
        request.setReorderLevel(10);

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.existsByVariant_VariantId(1L)).thenReturn(false);
        when(userRepository.findByEmail("admin@ecommerce.com")).thenReturn(Optional.of(mockUser));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        InventoryResponseDto result = inventoryService.initializeInventory(request);

        // Assert
        assertNotNull(result);
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("Test 7: initializeInventory throws exception if already initialized")
    void testInitializeInventory_AlreadyExists() {
        // Arrange
        InventoryCreateRequestDto request = new InventoryCreateRequestDto();
        request.setVariantId(1L);

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.existsByVariant_VariantId(1L)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> inventoryService.initializeInventory(request));
        verify(inventoryRepository, never()).save(any());
    }

    // =========================================================================
    // 5. RESTOCK INVENTORY
    // =========================================================================
    @Test
    @DisplayName("Test 8: restock adds quantity and logs transaction")
    void testRestock_Success() {
        // Arrange
        RestockRequestDto request = new RestockRequestDto();
        request.setVariantId(1L);
        request.setQuantity(20);
        request.setRemark("Batch 2 delivery");

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.of(mockInventory));
        when(userRepository.findByEmail("admin@ecommerce.com")).thenReturn(Optional.of(mockUser));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        InventoryResponseDto result = inventoryService.restock(request);

        // Assert
        assertNotNull(result);
        assertEquals(70, mockInventory.getQuantity()); // 50 + 20
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
        verify(inventoryRepository, times(1)).save(mockInventory);
    }

    // =========================================================================
    // 6. WRITE-OFF INVENTORY
    // =========================================================================
    @Test
    @DisplayName("Test 9: writeOff deducts quantity and logs WRITE_OFF transaction")
    void testWriteOff_Success() {
        // Arrange: 50 in stock, 5 reserved -> 45 available
        WriteOffRequestDto request = new WriteOffRequestDto();
        request.setVariantId(1L);
        request.setQuantity(5);
        request.setRemark("Water damaged packaging");

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.of(mockInventory));
        when(userRepository.findByEmail("admin@ecommerce.com")).thenReturn(Optional.of(mockUser));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        InventoryResponseDto result = inventoryService.writeOff(request);

        // Assert
        assertNotNull(result);
        assertEquals(45, mockInventory.getQuantity()); // 50 - 5
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("Test 10: writeOff throws exception when quantity exceeds available stock")
    void testWriteOff_InsufficientAvailableStock() {
        // Arrange: 50 in stock. Try to write off 60!
        WriteOffRequestDto request = new WriteOffRequestDto();
        request.setVariantId(1L);
        request.setQuantity(60);

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.of(mockInventory));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> inventoryService.writeOff(request));
        assertTrue(exception.getMessage().contains("Cannot write off 60 units"));
        verify(inventoryTransactionRepository, never()).save(any());
    }

    // =========================================================================
    // 7. STOCK ADJUSTMENT (AUDIT COUNT)
    // =========================================================================
    @Test
    @DisplayName("Test 11: adjustStock updates physical count correctly")
    void testAdjustStock_Success() {
        // Arrange: current stock 50, new count 52 (+2 adjustment)
        StockAdjustmentRequestDto request = new StockAdjustmentRequestDto();
        request.setVariantId(1L);
        request.setNewQuantity(52);
        request.setRemark("Annual audit recount");

        when(productVariantRepository.findById(1L)).thenReturn(Optional.of(mockVariant));
        when(inventoryRepository.findByVariant_VariantId(1L)).thenReturn(Optional.of(mockInventory));
        when(userRepository.findByEmail("admin@ecommerce.com")).thenReturn(Optional.of(mockUser));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(mockInventory);
        when(inventoryMapper.toResponseDto(mockInventory)).thenReturn(mockInventoryDto);

        // Act
        InventoryResponseDto result = inventoryService.adjustStock(request);

        // Assert
        assertNotNull(result);
        assertEquals(52, mockInventory.getQuantity());
        verify(inventoryTransactionRepository, times(1)).save(any(InventoryTransaction.class));
    }

    // =========================================================================
    // 8. TRANSACTION AUDIT HISTORY
    // =========================================================================
    @Test
    @DisplayName("Test 12: getTransactionHistory returns audit trail sorted by date")
    void testGetTransactionHistory_All() {
        // Arrange
        InventoryTransaction mockTxn = InventoryTransaction.builder()
                .txnId(1L)
                .txnType("RESTOCK")
                .qty(20)
                .beforeQty(30)
                .afterQty(50)
                .createdAt(LocalDateTime.now())
                .build();

        InventoryTransactionResponseDto txnDto = InventoryTransactionResponseDto.builder()
                .txnId(1L)
                .txnType("RESTOCK")
                .qty(20)
                .build();

        when(inventoryTransactionRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(mockTxn));
        when(transactionMapper.toResponseDto(mockTxn)).thenReturn(txnDto);

        // Act
        List<InventoryTransactionResponseDto> result = inventoryService.getTransactionHistory(null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("RESTOCK", result.get(0).getTxnType());
        verify(inventoryTransactionRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }
}
