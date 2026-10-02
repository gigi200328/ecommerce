package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.repository.VariationRepository;
import com.ojt.ecommerce.entity.Variation;
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
@DisplayName("VariationServiceImpl Unit Tests")
class VariationServiceImplTest {

    @Mock
    private VariationRepository variationRepository;

    @InjectMocks
    private VariationServiceImpl variationService;

    private Variation mockVariation;

    @BeforeEach
    void setUp() {
        mockVariation = Variation.builder()
                .variationId(1L)
                .name("Size")
                .build();
    }

    @Test
    @DisplayName("Test 1: createVariation should save and return variation successfully")
    void testCreateVariation_Success() {
        when(variationRepository.existsByName("Size")).thenReturn(false);
        when(variationRepository.save(any(Variation.class))).thenReturn(mockVariation);

        Variation result = variationService.createVariation(mockVariation);

        assertNotNull(result);
        assertEquals("Size", result.getName());
        verify(variationRepository, times(1)).save(mockVariation);
    }

    @Test
    @DisplayName("Test 2: createVariation throws RuntimeException when name already exists")
    void testCreateVariation_DuplicateName() {
        when(variationRepository.existsByName("Size")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> variationService.createVariation(mockVariation));
        assertEquals("Variation already exists.", exception.getMessage());
        verify(variationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 3: getAllVariations should return all variations")
    void testGetAllVariations() {
        when(variationRepository.findAll()).thenReturn(List.of(mockVariation));

        List<Variation> list = variationService.getAllVariations();

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Size", list.get(0).getName());
        verify(variationRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Test 4: getVariationById returns variation if found")
    void testGetVariationById_Success() {
        when(variationRepository.findById(1L)).thenReturn(Optional.of(mockVariation));

        Variation result = variationService.getVariationById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getVariationId());
        assertEquals("Size", result.getName());
    }

    @Test
    @DisplayName("Test 5: getVariationById throws RuntimeException when not found")
    void testGetVariationById_NotFound() {
        when(variationRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> variationService.getVariationById(99L));
        assertEquals("Variation not found.", exception.getMessage());
    }

    @Test
    @DisplayName("Test 6: updateVariation modifies variation name and saves")
    void testUpdateVariation_Success() {
        Variation updateData = Variation.builder().name("Shoe Size").build();
        when(variationRepository.findById(1L)).thenReturn(Optional.of(mockVariation));
        when(variationRepository.save(any(Variation.class))).thenReturn(mockVariation);

        Variation updated = variationService.updateVariation(1L, updateData);

        assertNotNull(updated);
        assertEquals("Shoe Size", mockVariation.getName());
        verify(variationRepository, times(1)).save(mockVariation);
    }

    @Test
    @DisplayName("Test 7: deleteVariation removes entity")
    void testDeleteVariation_Success() {
        when(variationRepository.findById(1L)).thenReturn(Optional.of(mockVariation));
        doNothing().when(variationRepository).delete(mockVariation);

        variationService.deleteVariation(1L);

        verify(variationRepository, times(1)).delete(mockVariation);
    }
}
