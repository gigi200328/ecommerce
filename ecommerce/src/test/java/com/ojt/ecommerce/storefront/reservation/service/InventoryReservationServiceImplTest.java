package com.ojt.ecommerce.storefront.reservation.service;

import com.ojt.ecommerce.entity.Inventory;
import com.ojt.ecommerce.entity.Order;
import com.ojt.ecommerce.entity.OrderItem;
import com.ojt.ecommerce.entity.ProductVariant;
import com.ojt.ecommerce.storefront.catalog.repository.G5InventoryRepository;
import com.ojt.ecommerce.storefront.checkout.repository.G5OrderItemRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.reservation.dto.ReservationItem;
import com.ojt.ecommerce.storefront.reservation.entity.InventoryReservation;
import com.ojt.ecommerce.storefront.reservation.repository.InventoryReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryReservationServiceImplTest {

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private G5InventoryRepository inventoryRepository;

    @Mock
    private G5OrderItemRepository orderItemRepository;

    @InjectMocks
    private InventoryReservationServiceImpl reservationService;

    private Order mockOrder;
    private ProductVariant variant1;
    private ProductVariant variant2;
    private Inventory inventory1;
    private Inventory inventory2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservationService, "reservationTtlMinutes", 3);

        mockOrder = Order.builder()
                .orderId(101L)
                .orderNo("ORD-TEST-101")
                .build();

        variant1 = ProductVariant.builder()
                .variantId(1L)
                .sku("SKU-VAR-1")
                .build();

        variant2 = ProductVariant.builder()
                .variantId(2L)
                .sku("SKU-VAR-2")
                .build();

        inventory1 = Inventory.builder()
                .inventoryId(10L)
                .variant(variant1)
                .quantity(50)
                .reservedQuantity(0)
                .build();

        inventory2 = Inventory.builder()
                .inventoryId(20L)
                .variant(variant2)
                .quantity(30)
                .reservedQuantity(5)
                .build();
    }

    // =========================================================================
    // 1. reserveStockForOrder Tests
    // =========================================================================

    @Test
    @DisplayName("Phase 7 - Test 1: reserveStockForOrder creates reservation with TTL and syncs reserved_quantity")
    void reserveStockForOrder_success_createsReservationWithTtl_andSyncsReservedQuantity() {
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(0);
        when(reservationRepository.findByOrderOrderIdAndVariantVariantId(101L, 1L)).thenReturn(Optional.empty());

        ReservationItem item = ReservationItem.builder()
                .variant(variant1)
                .quantity(5)
                .build();

        reservationService.reserveStockForOrder(mockOrder, List.of(item));

        ArgumentCaptor<InventoryReservation> resCaptor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservationRepository).save(resCaptor.capture());
        InventoryReservation savedRes = resCaptor.getValue();

        assertEquals(101L, savedRes.getOrder().getOrderId());
        assertEquals(1L, savedRes.getVariant().getVariantId());
        assertEquals(5, savedRes.getQuantity());
        assertEquals(InventoryReservation.STATUS_RESERVED, savedRes.getStatus());
        assertNotNull(savedRes.getExpiresAt());
        assertTrue(savedRes.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(2)));
        assertTrue(savedRes.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(4)));

        // Verify inventory.reserved_quantity synced to 5
        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(invCaptor.capture());
        assertEquals(5, invCaptor.getValue().getReservedQuantity());
    }

    @Test
    @DisplayName("Phase 7 - Test 2: reserveStockForOrder throws InvalidRequestException when requested exceeds available stock")
    void reserveStockForOrder_insufficientStock_throwsInvalidRequestException() {
        inventory1.setQuantity(10);
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(8);

        ReservationItem item = ReservationItem.builder()
                .variant(variant1)
                .quantity(3) // available is 10 - 8 = 2, requested 3
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                reservationService.reserveStockForOrder(mockOrder, List.of(item)));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phase 7 - Test 3: reserveStockForOrder sorts items by variantId to prevent deadlocks")
    void reserveStockForOrder_deterministicLockOrdering_sortsByVariantId() {
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(inventoryRepository.findByVariantIdForUpdate(2L)).thenReturn(Optional.of(inventory2));
        when(reservationRepository.sumActiveReservedQuantityByVariant(anyLong(), any(LocalDateTime.class))).thenReturn(0);

        ReservationItem item2 = ReservationItem.builder().variant(variant2).quantity(1).build();
        ReservationItem item1 = ReservationItem.builder().variant(variant1).quantity(2).build();

        // Pass in reverse order (variant 2 then variant 1)
        reservationService.reserveStockForOrder(mockOrder, List.of(item2, item1));

        // Verify that lock acquisition (findByVariantIdForUpdate) was ordered 1L then 2L
        org.mockito.InOrder inOrder = inOrder(inventoryRepository);
        inOrder.verify(inventoryRepository).findByVariantIdForUpdate(1L);
        inOrder.verify(inventoryRepository).findByVariantIdForUpdate(2L);
    }

    @Test
    @DisplayName("Phase 7 - Test 4: reserveStockForOrder updates existing reservation if already present (idempotent)")
    void reserveStockForOrder_idempotent_updatesExistingReservationInsteadOfDuplicating() {
        InventoryReservation existing = InventoryReservation.builder()
                .reservationId(500L)
                .order(mockOrder)
                .variant(variant1)
                .quantity(2)
                .status(InventoryReservation.STATUS_RESERVED)
                .build();

        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(2);
        when(reservationRepository.findByOrderOrderIdAndVariantVariantId(101L, 1L)).thenReturn(Optional.of(existing));

        ReservationItem item = ReservationItem.builder().variant(variant1).quantity(4).build();

        reservationService.reserveStockForOrder(mockOrder, List.of(item));

        verify(reservationRepository).save(existing);
        assertEquals(4, existing.getQuantity());
        assertEquals(InventoryReservation.STATUS_RESERVED, existing.getStatus());
    }

    @Test
    @DisplayName("Phase 7 - Test 5: reserveStockForOrder rejects zero or negative quantity")
    void reserveStockForOrder_zeroOrNegativeQuantity_throwsInvalidRequestException() {
        ReservationItem item = ReservationItem.builder().variant(variant1).quantity(0).build();

        assertThrows(InvalidRequestException.class, () ->
                reservationService.reserveStockForOrder(mockOrder, List.of(item)));
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    @DisplayName("Phase 7 - Test 6: reserveStockForOrder throws when inventory record is missing")
    void reserveStockForOrder_missingInventory_throwsInvalidRequestException() {
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.empty());

        ReservationItem item = ReservationItem.builder().variant(variant1).quantity(1).build();

        assertThrows(InvalidRequestException.class, () ->
                reservationService.reserveStockForOrder(mockOrder, List.of(item)));
    }

    // =========================================================================
    // 2. consumeReservationsForOrder Tests (Payment SUCCESS)
    // =========================================================================

    @Test
    @DisplayName("Phase 7 - Test 7: consumeReservations marks status CONSUMED and syncs reserved_quantity")
    void consumeReservationsForOrder_success_updatesStatusConsumed_andSyncsReservedQuantity() {
        InventoryReservation res = InventoryReservation.builder()
                .reservationId(1L)
                .order(mockOrder)
                .variant(variant1)
                .quantity(5)
                .status(InventoryReservation.STATUS_RESERVED)
                .build();

        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(new ArrayList<>(List.of(res)));
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        reservationService.consumeReservationsForOrder(101L);

        assertEquals(InventoryReservation.STATUS_CONSUMED, res.getStatus());
        verify(reservationRepository).save(res);

        // reservedQuantity synced to remaining 0
        assertEquals(0, inventory1.getReservedQuantity());
        verify(inventoryRepository).save(inventory1);
    }

    @Test
    @DisplayName("Phase 7 - Test 8: consumeReservations is noop when order has no active reservations")
    void consumeReservationsForOrder_noReservations_noop() {
        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(Collections.emptyList());

        reservationService.consumeReservationsForOrder(101L);

        verify(reservationRepository, never()).save(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phase 7 - Test 9: Duplicate consumeReservations is idempotent")
    void consumeReservationsForOrder_duplicateInvocation_isIdempotent() {
        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(Collections.emptyList());

        reservationService.consumeReservationsForOrder(101L);
        reservationService.consumeReservationsForOrder(101L);

        verify(reservationRepository, never()).save(any());
    }

    // =========================================================================
    // 3. releaseReservationsForOrder Tests (Payment FAILED / Abort)
    // =========================================================================

    @Test
    @DisplayName("Phase 7 - Test 10: releaseReservations marks status RELEASED and syncs reserved_quantity")
    void releaseReservationsForOrder_success_updatesStatusReleased_andSyncsReservedQuantity() {
        InventoryReservation res = InventoryReservation.builder()
                .reservationId(1L)
                .order(mockOrder)
                .variant(variant1)
                .quantity(5)
                .status(InventoryReservation.STATUS_RESERVED)
                .build();

        inventory1.setReservedQuantity(5);

        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(new ArrayList<>(List.of(res)));
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        reservationService.releaseReservationsForOrder(101L);

        assertEquals(InventoryReservation.STATUS_RELEASED, res.getStatus());
        verify(reservationRepository).save(res);

        assertEquals(0, inventory1.getReservedQuantity());
        verify(inventoryRepository).save(inventory1);
    }

    @Test
    @DisplayName("Phase 7 - Test 11: releaseReservations is noop when no active reservations exist")
    void releaseReservationsForOrder_noReservations_noop() {
        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(Collections.emptyList());

        reservationService.releaseReservationsForOrder(101L);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phase 7 - Test 12: Duplicate releaseReservations is idempotent")
    void releaseReservationsForOrder_duplicateInvocation_isIdempotent() {
        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(Collections.emptyList());

        reservationService.releaseReservationsForOrder(101L);
        reservationService.releaseReservationsForOrder(101L);

        verify(reservationRepository, never()).save(any());
    }

    // =========================================================================
    // 4. expireAbandonedReservations Tests (Worker)
    // =========================================================================

    @Test
    @DisplayName("Phase 7 - Test 13: expireAbandonedReservations marks overdue EXPIRED and syncs reserved_quantity")
    void expireAbandonedReservations_expiresOverdue_andSyncsReservedQuantity() {
        InventoryReservation expiredRes = InventoryReservation.builder()
                .reservationId(10L)
                .order(mockOrder)
                .variant(variant1)
                .quantity(3)
                .status(InventoryReservation.STATUS_RESERVED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        inventory1.setReservedQuantity(3);

        when(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredRes));
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        int count = reservationService.expireAbandonedReservations();

        assertEquals(1, count);
        assertEquals(InventoryReservation.STATUS_EXPIRED, expiredRes.getStatus());
        verify(reservationRepository).save(expiredRes);

        assertEquals(0, inventory1.getReservedQuantity());
        verify(inventoryRepository).save(inventory1);
    }

    @Test
    @DisplayName("Phase 7 - Test 14: expireAbandonedReservations returns 0 when no expired reservations exist")
    void expireAbandonedReservations_noExpired_returnsZero() {
        when(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        int count = reservationService.expireAbandonedReservations();

        assertEquals(0, count);
        verify(reservationRepository, never()).save(any());
    }

    // =========================================================================
    // 5. getAvailableStock & getAvailableStockForVariants Tests
    // =========================================================================

    @Test
    @DisplayName("Phase 7 - Test 15: getAvailableStock calculates physical minus active reservations")
    void getAvailableStock_calculatesPhysicalMinusActiveReservations() {
        inventory1.setQuantity(20);
        when(inventoryRepository.findByVariantVariantId(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(7);

        int available = reservationService.getAvailableStock(1L);

        assertEquals(13, available); // 20 - 7
    }

    @Test
    @DisplayName("Phase 7 - Test 16: getAvailableStock returns zero when reserved exceeds or equals physical")
    void getAvailableStock_whenReservedEqualsPhysical_returnsZero() {
        inventory1.setQuantity(10);
        when(inventoryRepository.findByVariantVariantId(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(10);

        int available = reservationService.getAvailableStock(1L);

        assertEquals(0, available);
    }

    @Test
    @DisplayName("Phase 7 - Test 17: getAvailableStock returns zero when inventory record does not exist")
    void getAvailableStock_whenNoInventory_returnsZero() {
        when(inventoryRepository.findByVariantVariantId(999L)).thenReturn(Optional.empty());

        int available = reservationService.getAvailableStock(999L);

        assertEquals(0, available);
    }

    @Test
    @DisplayName("Phase 7 - Test 18: getAvailableStock returns zero when variantId is null")
    void getAvailableStock_nullVariantId_returnsZero() {
        assertEquals(0, reservationService.getAvailableStock(null));
        verifyNoInteractions(inventoryRepository);
    }

    @Test
    @DisplayName("Phase 7 - Test 19: getAvailableStockForVariants calculates available stock across all variants")
    void getAvailableStockForVariants_mapsAllVariantsAccurately() {
        inventory1.setQuantity(50);
        inventory2.setQuantity(30);

        when(inventoryRepository.findByVariantVariantIdIn(List.of(1L, 2L)))
                .thenReturn(List.of(inventory1, inventory2));

        // variant 1 has 10 active reserved, variant 2 has 5 active reserved
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, 10L});
        rows.add(new Object[]{2L, 5L});

        when(reservationRepository.sumActiveReservedQuantityByVariantIds(eq(List.of(1L, 2L)), any(LocalDateTime.class)))
                .thenReturn(rows);

        Map<Long, Integer> availableMap = reservationService.getAvailableStockForVariants(List.of(1L, 2L));

        assertNotNull(availableMap);
        assertEquals(40, availableMap.get(1L)); // 50 - 10
        assertEquals(25, availableMap.get(2L)); // 30 - 5
    }

    @Test
    @DisplayName("Phase 7 - Test 20: getAvailableStockForVariants returns empty map on null or empty input")
    void getAvailableStockForVariants_nullOrEmpty_returnsEmptyMap() {
        assertTrue(reservationService.getAvailableStockForVariants(null).isEmpty());
        assertTrue(reservationService.getAvailableStockForVariants(Collections.emptyList()).isEmpty());
        verifyNoInteractions(inventoryRepository);
    }

    // =========================================================================
    // Phase 8: Reservation Expiry Query & Active State Tests
    // =========================================================================

    @Test
    @DisplayName("Phase 8 - Test 1: getReservationExpiryForOrder returns earliest expiresAt")
    void getReservationExpiryForOrder_success_returnsEarliestExpiresAt() {
        LocalDateTime t1 = LocalDateTime.now().plusMinutes(2);
        LocalDateTime t2 = LocalDateTime.now().plusMinutes(3);

        InventoryReservation r1 = InventoryReservation.builder()
                .reservationId(1L)
                .expiresAt(t2)
                .status(InventoryReservation.STATUS_RESERVED)
                .build();
        InventoryReservation r2 = InventoryReservation.builder()
                .reservationId(2L)
                .expiresAt(t1)
                .status(InventoryReservation.STATUS_RESERVED)
                .build();

        when(reservationRepository.findByOrderOrderId(101L)).thenReturn(List.of(r1, r2));

        Optional<LocalDateTime> expiryOpt = reservationService.getReservationExpiryForOrder(101L);

        assertTrue(expiryOpt.isPresent());
        assertEquals(t1, expiryOpt.get());
    }

    @Test
    @DisplayName("Phase 8 - Test 2: getReservationExpiryForOrder returns empty when null orderId or no reservations")
    void getReservationExpiryForOrder_emptyWhenNoReservationsOrNull() {
        assertTrue(reservationService.getReservationExpiryForOrder(null).isEmpty());

        when(reservationRepository.findByOrderOrderId(999L)).thenReturn(Collections.emptyList());
        assertTrue(reservationService.getReservationExpiryForOrder(999L).isEmpty());
    }

    @Test
    @DisplayName("Phase 8 - Test 3: isReservationActiveForOrder returns true when active reservation exists in future")
    void isReservationActiveForOrder_trueWhenActiveReservationExists() {
        LocalDateTime future = LocalDateTime.now().plusMinutes(3);
        InventoryReservation r = InventoryReservation.builder()
                .reservationId(1L)
                .status(InventoryReservation.STATUS_RESERVED)
                .expiresAt(future)
                .build();

        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(List.of(r));

        assertTrue(reservationService.isReservationActiveForOrder(101L));
    }

    @Test
    @DisplayName("Phase 8 - Test 4: isReservationActiveForOrder returns false when expired or no reserved records")
    void isReservationActiveForOrder_falseWhenExpiredOrNone() {
        assertFalse(reservationService.isReservationActiveForOrder(null));

        when(reservationRepository.findByOrderOrderIdAndStatus(101L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(Collections.emptyList());
        assertFalse(reservationService.isReservationActiveForOrder(101L));

        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        InventoryReservation expiredRes = InventoryReservation.builder()
                .reservationId(1L)
                .status(InventoryReservation.STATUS_RESERVED)
                .expiresAt(past)
                .build();
        when(reservationRepository.findByOrderOrderIdAndStatus(102L, InventoryReservation.STATUS_RESERVED))
                .thenReturn(List.of(expiredRes));
        assertFalse(reservationService.isReservationActiveForOrder(102L));
    }

    // =========================================================================
    // Phase 9: reReserveStockForOrder Tests
    // =========================================================================

    @Test
    @DisplayName("Phase 9 - Test 1: reReserveStockForOrder succeeds, resets 3-min TTL, and updates reserved_quantity")
    void reReserveStockForOrder_success_updatesReservationAndSyncsReservedQuantity() {
        OrderItem item = OrderItem.builder()
                .variant(variant1)
                .qty(5)
                .build();
        when(orderItemRepository.findByOrderOrderId(101L)).thenReturn(List.of(item));
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(inventory1));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(0);

        InventoryReservation existingExpired = InventoryReservation.builder()
                .reservationId(55L)
                .order(mockOrder)
                .variant(variant1)
                .quantity(5)
                .status(InventoryReservation.STATUS_EXPIRED)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(reservationRepository.findByOrderOrderIdAndVariantVariantId(101L, 1L))
                .thenReturn(Optional.of(existingExpired));

        reservationService.reReserveStockForOrder(mockOrder);

        ArgumentCaptor<InventoryReservation> resCaptor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservationRepository).save(resCaptor.capture());
        InventoryReservation savedRes = resCaptor.getValue();

        assertEquals(55L, savedRes.getReservationId());
        assertEquals(InventoryReservation.STATUS_RESERVED, savedRes.getStatus());
        assertEquals(5, savedRes.getQuantity());
        assertTrue(savedRes.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(2)));
        assertTrue(savedRes.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(4)));

        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(invCaptor.capture());
        assertEquals(5, invCaptor.getValue().getReservedQuantity());
    }

    @Test
    @DisplayName("Phase 9 - Test 2: reReserveStockForOrder throws InvalidRequestException when available stock insufficient")
    void reReserveStockForOrder_throwsException_whenStockInsufficient() {
        OrderItem item = OrderItem.builder()
                .variant(variant1)
                .qty(10)
                .build();
        when(orderItemRepository.findByOrderOrderId(101L)).thenReturn(List.of(item));

        Inventory lowInv = Inventory.builder()
                .inventoryId(10L)
                .variant(variant1)
                .quantity(5)
                .reservedQuantity(0)
                .build();
        when(inventoryRepository.findByVariantIdForUpdate(1L)).thenReturn(Optional.of(lowInv));
        when(reservationRepository.sumActiveReservedQuantityByVariant(eq(1L), any(LocalDateTime.class))).thenReturn(0);
        when(reservationRepository.findByOrderOrderIdAndVariantVariantId(101L, 1L)).thenReturn(Optional.empty());

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> reservationService.reReserveStockForOrder(mockOrder));

        assertTrue(ex.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Phase 9 - Test 3: reReserveStockForOrder throws InvalidRequestException on null order or empty items")
    void reReserveStockForOrder_throwsException_onNullOrEmpty() {
        assertThrows(InvalidRequestException.class, () -> reservationService.reReserveStockForOrder(null));

        when(orderItemRepository.findByOrderOrderId(101L)).thenReturn(Collections.emptyList());
        assertThrows(InvalidRequestException.class, () -> reservationService.reReserveStockForOrder(mockOrder));
    }
}
