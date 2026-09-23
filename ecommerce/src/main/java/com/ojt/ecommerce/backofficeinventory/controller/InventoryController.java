package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.dto.InventoryCreateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.InventoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.InventoryTransactionResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.RestockRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.StockAdjustmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.WriteOffRequestDto;
import com.ojt.ecommerce.backofficeinventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Back-Office APIs for managing stock, low-stock alerts, restock, write-off, and inventory transactions")
public class InventoryController {

    private final InventoryService inventoryService;

    // GET ALL
    @Operation(summary = "Get all inventory items", description = "Retrieves current stock levels, reserved quantities, and statuses for all variants.")
    @ApiResponse(responseCode = "200", description = "Inventory list retrieved successfully")
    @GetMapping
    public ResponseEntity<List<InventoryResponseDto>> getAllInventory() {
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    // GET BY VARIANT ID
    @Operation(summary = "Get inventory by product variant ID", description = "Retrieves stock details for a specific variant. If not initialized, creates an initial record with 0 stock.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product variant not found")
    })
    @GetMapping("/variant/{variantId}")
    public ResponseEntity<InventoryResponseDto> getInventoryByVariantId(@PathVariable Long variantId) {
        return ResponseEntity.ok(inventoryService.getInventoryByVariantId(variantId));
    }

    // GET BY ID
    @Operation(summary = "Get inventory by inventory ID", description = "Retrieves stock details by primary inventory ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory details retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Inventory record not found")
    })
    @GetMapping("/{inventoryId}")
    public ResponseEntity<InventoryResponseDto> getInventoryById(@PathVariable Long inventoryId) {
        return ResponseEntity.ok(inventoryService.getInventoryById(inventoryId));
    }

    // LOW STOCK ALERT
    @Operation(summary = "Get low stock alerts", description = "Returns all variants where physical quantity is at or below the reorder level, or marked as LOW_STOCK / OUT_OF_STOCK.")
    @ApiResponse(responseCode = "200", description = "Low stock items retrieved successfully")
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponseDto>> getLowStockAlerts() {
        return ResponseEntity.ok(inventoryService.getLowStockAlerts());
    }

    // INITIALIZE INVENTORY
    @Operation(summary = "Initialize inventory tracking for a variant", description = "Sets initial stock level and reorder threshold for a product variant.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Inventory tracking initialized successfully"),
        @ApiResponse(responseCode = "400", description = "Inventory already initialized or invalid data"),
        @ApiResponse(responseCode = "404", description = "Product variant not found")
    })
    @PostMapping("/initialize")
    public ResponseEntity<InventoryResponseDto> initializeInventory(
            @Valid @RequestBody InventoryCreateRequestDto request) {
        return new ResponseEntity<>(inventoryService.initializeInventory(request), HttpStatus.CREATED);
    }

    // RESTOCK
    @Operation(summary = "Restock inventory", description = "Adds new units to inventory, updates stock status, and logs a RESTOCK transaction in audit history.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventory restocked successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = InventoryResponseDto.class),
                examples = @ExampleObject(name = "Restock Example", value = """
                    {
                      "inventoryId": 1,
                      "variantId": 1,
                      "sku": "NIKE-AIR-BLK-42",
                      "productName": "Nike Air Max",
                      "quantity": 50,
                      "reservedQuantity": 0,
                      "availableQuantity": 50,
                      "reorderLevel": 10,
                      "status": "NORMAL"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Product variant not found")
    })
    @PostMapping("/restock")
    public ResponseEntity<InventoryResponseDto> restock(
            @Valid @RequestBody RestockRequestDto request) {
        return ResponseEntity.ok(inventoryService.restock(request));
    }

    // WRITE-OFF
    @Operation(summary = "Write off damaged or expired stock", description = "Deducts stock units due to damage, expiration, or loss, updates status, and logs a WRITE_OFF transaction.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Write-off processed successfully"),
        @ApiResponse(responseCode = "400", description = "Write-off quantity exceeds current available stock"),
        @ApiResponse(responseCode = "404", description = "Product variant or inventory not found")
    })
    @PostMapping("/write-off")
    public ResponseEntity<InventoryResponseDto> writeOff(
            @Valid @RequestBody WriteOffRequestDto request) {
        return ResponseEntity.ok(inventoryService.writeOff(request));
    }

    // STOCK ADJUSTMENT
    @Operation(summary = "Adjust stock quantity", description = "Updates stock to match physical inventory count after audit and logs an ADJUSTMENT transaction.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock adjusted successfully"),
        @ApiResponse(responseCode = "404", description = "Product variant not found")
    })
    @PostMapping("/adjust")
    public ResponseEntity<InventoryResponseDto> adjustStock(
            @Valid @RequestBody StockAdjustmentRequestDto request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }

    // TRANSACTION HISTORY
    @Operation(summary = "Get inventory transaction history", description = "Retrieves audit trail of all inventory changes, optionally filtered by transaction type (RESTOCK, SALE, RETURN, WRITE_OFF, ADJUSTMENT).")
    @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully")
    @GetMapping("/transactions")
    public ResponseEntity<List<InventoryTransactionResponseDto>> getTransactionHistory(
            @RequestParam(required = false) String txnType) {
        return ResponseEntity.ok(inventoryService.getTransactionHistory(null, txnType));
    }

    // TRANSACTION HISTORY BY VARIANT
    @Operation(summary = "Get transaction history for a specific variant", description = "Retrieves all stock transactions (restocks, sales, write-offs) for a specific product variant.")
    @ApiResponse(responseCode = "200", description = "Variant transaction history retrieved successfully")
    @GetMapping("/transactions/variant/{variantId}")
    public ResponseEntity<List<InventoryTransactionResponseDto>> getTransactionsByVariant(
            @PathVariable Long variantId) {
        return ResponseEntity.ok(inventoryService.getTransactionHistory(variantId, null));
    }
}
