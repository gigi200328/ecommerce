package com.ojt.ecommerce.backofficeinventory.service;

import java.util.List;

import com.ojt.ecommerce.backofficeinventory.dto.InventoryCreateRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.InventoryResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.InventoryTransactionResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.RestockRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.StockAdjustmentRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.WriteOffRequestDto;

public interface InventoryService {

    List<InventoryResponseDto> getAllInventory();

    InventoryResponseDto getInventoryByVariantId(Long variantId);

    InventoryResponseDto getInventoryById(Long inventoryId);

    List<InventoryResponseDto> getLowStockAlerts();

    InventoryResponseDto initializeInventory(InventoryCreateRequestDto request);

    InventoryResponseDto restock(RestockRequestDto request);

    InventoryResponseDto writeOff(WriteOffRequestDto request);

    InventoryResponseDto adjustStock(StockAdjustmentRequestDto request);

    List<InventoryTransactionResponseDto> getTransactionHistory(Long variantId, String txnType);
}
