package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.repository.VariationOptionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.VariationRepository;
import com.ojt.ecommerce.entity.Variation;
import com.ojt.ecommerce.entity.VariationOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VariationOptionServiceImpl Unit Tests")
class VariationOptionServiceImplTest {

    @Mock
    private VariationOptionRepository variationOptionRepository;

    @Mock
    private VariationRepository variationRepository;

    @InjectMocks
    private VariationOptionServiceImpl variationOptionService;

    private Variation mockVariation;
    private VariationOption mockOption;

    @BeforeEach
    void setUp() {
        mockVariation = Variation.builder()
                .variationId(1L)
                .name("Color")
                .build();

        mockOption = VariationOption.builder()
                .optionId(10L)
                .variation(mockVariation)
                .value("Red")
                .build();
    }

    @Test
    @DisplayName("Test 1: createVariationOption creates option when valid")
    void testCreateVariationOption_Success() {
        when(variationRepository.findById(1L)).thenReturn(Optional.of(mockVariation));
        when(variationOptionRepository.existsByVariation_VariationIdAndValue(1L, "Red")).thenReturn(false);
        when(variationOptionRepository.save(any(VariationOption.class))).thenReturn(mockOption);

        VariationOption result = variationOptionService.createVariationOption(mockOption);

        assertNotNull(result);
        assertEquals("Red", result.getValue());
        verify(variationOptionRepository, times(1)).save(mockOption);
    }

    @Test
    @DisplayName("Test 2: createVariationOption throws exception when variation is null")
    void testCreateVariationOption_MissingVariation() {
        VariationOption optionNoVar = VariationOption.builder().value("Red").build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> variationOptionService.createVariationOption(optionNoVar));
        assertEquals("Variation ID is required.", ex.getMessage());
    }

    @Test
    @DisplayName("Test 3: createVariationOption throws exception when variation does not exist in DB")
    void testCreateVariationOption_VariationNotFound() {
        when(variationRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> variationOptionService.createVariationOption(mockOption));
        assertTrue(ex.getMessage().contains("Variation not found with id: 1"));
    }

    @Test
    @DisplayName("Test 4: createVariationOption throws exception on duplicate value for variation")
    void testCreateVariationOption_DuplicateValue() {
        when(variationRepository.findById(1L)).thenReturn(Optional.of(mockVariation));
        when(variationOptionRepository.existsByVariation_VariationIdAndValue(1L, "Red")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> variationOptionService.createVariationOption(mockOption));
        assertEquals("This option already exists for this variation.", ex.getMessage());
        verify(variationOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 5: getAllVariationOptions returns all records")
    void testGetAllVariationOptions() {
        when(variationOptionRepository.findAll()).thenReturn(List.of(mockOption));

        List<VariationOption> list = variationOptionService.getAllVariationOptions();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Red", list.get(0).getValue());
    }

    @Test
    @DisplayName("Test 6: getVariationOptionById returns entity if found")
    void testGetVariationOptionById_Success() {
        when(variationOptionRepository.findById(10L)).thenReturn(Optional.of(mockOption));

        VariationOption result = variationOptionService.getVariationOptionById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getOptionId());
    }

    @Test
    @DisplayName("Test 7: getVariationOptionById throws exception when not found")
    void testGetVariationOptionById_NotFound() {
        when(variationOptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> variationOptionService.getVariationOptionById(99L));
    }

    @Test
    @DisplayName("Test 8: getOptionsByVariationId returns options for valid variation")
    void testGetOptionsByVariationId_Success() {
        when(variationRepository.existsById(1L)).thenReturn(true);
        when(variationOptionRepository.findByVariation_VariationId(1L)).thenReturn(List.of(mockOption));

        List<VariationOption> result = variationOptionService.getOptionsByVariationId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test 9: getOptionsByVariationId throws exception if variation not found")
    void testGetOptionsByVariationId_VariationNotFound() {
        when(variationRepository.existsById(99L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> variationOptionService.getOptionsByVariationId(99L));
    }

    @Test
    @DisplayName("Test 10: updateVariationOption updates value successfully")
    void testUpdateVariationOption_Success() {
        VariationOption updatePayload = VariationOption.builder().value("Crimson").build();

        when(variationOptionRepository.findById(10L)).thenReturn(Optional.of(mockOption));
        when(variationOptionRepository.existsByVariation_VariationIdAndValue(1L, "Crimson")).thenReturn(false);
        when(variationOptionRepository.save(any(VariationOption.class))).thenReturn(mockOption);

        VariationOption updated = variationOptionService.updateVariationOption(10L, updatePayload);

        assertNotNull(updated);
        assertEquals("Crimson", mockOption.getValue());
        verify(variationOptionRepository, times(1)).save(mockOption);
    }

    @Test
    @DisplayName("Test 11: updateVariationOption throws exception if new value duplicates existing option")
    void testUpdateVariationOption_DuplicateValue() {
        VariationOption updatePayload = VariationOption.builder().value("Blue").build();

        when(variationOptionRepository.findById(10L)).thenReturn(Optional.of(mockOption));
        when(variationOptionRepository.existsByVariation_VariationIdAndValue(1L, "Blue")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> variationOptionService.updateVariationOption(10L, updatePayload));
        assertEquals("This option already exists for this variation.", ex.getMessage());
        verify(variationOptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 12: deleteVariationOption deletes option")
    void testDeleteVariationOption_Success() {
        when(variationOptionRepository.findById(10L)).thenReturn(Optional.of(mockOption));
        doNothing().when(variationOptionRepository).delete(mockOption);

        variationOptionService.deleteVariationOption(10L);

        verify(variationOptionRepository, times(1)).delete(mockOption);
    }
}
