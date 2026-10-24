package com.ojt.ecommerce.storefront.profile.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.CustomerAddress;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.exception.InvalidRequestException;
import com.ojt.ecommerce.storefront.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressResponse;
import com.ojt.ecommerce.storefront.profile.mapper.CustomerAddressMapper;
import com.ojt.ecommerce.storefront.profile.repository.CustomerAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceImplTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAddressMapper addressMapper;

    @InjectMocks
    private CustomerAddressServiceImpl addressService;

    private Customer customer;
    private CustomerAddress address1;
    private CustomerAddress address2;
    private CustomerAddressRequest request;
    private CustomerAddressResponse response1;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .customerId(1L)
                .fullName("John Doe")
                .email("john@example.com")
                .build();

        address1 = CustomerAddress.builder()
                .addressId(10L)
                .customer(customer)
                .label("Home")
                .recipientName("John Doe")
                .phoneNumber("09123456789")
                .addressLine1("No. 12, Main Street")
                .city("Yangon")
                .township("Kyauktada")
                .isDefault(true)
                .build();

        address2 = CustomerAddress.builder()
                .addressId(20L)
                .customer(customer)
                .label("Office")
                .recipientName("John Doe")
                .phoneNumber("09987654321")
                .addressLine1("No. 45, Office Tower")
                .city("Yangon")
                .township("Kamayut")
                .isDefault(false)
                .build();

        request = new CustomerAddressRequest();
        request.setLabel("Home");
        request.setRecipientName("John Doe");
        request.setPhoneNumber("09123456789");
        request.setAddressLine1("No. 12, Main Street");
        request.setCity("Yangon");
        request.setTownship("Kyauktada");
        request.setIsDefault(true);

        response1 = new CustomerAddressResponse();
        response1.setAddressId(10L);
        response1.setLabel("Home");
        response1.setRecipientName("John Doe");
        response1.setPhoneNumber("09123456789");
        response1.setAddressLine1("No. 12, Main Street");
        response1.setCity("Yangon");
        response1.setTownship("Kyauktada");
        response1.setIsDefault(true);
    }

    // =========================================================================
    // 1. getAddresses Tests
    // =========================================================================

    @Test
    @DisplayName("Should return list of customer addresses mapped to DTOs")
    void getAddresses_Success() {
        when(addressRepository.findByCustomerCustomerId(1L)).thenReturn(List.of(address1, address2));
        when(addressMapper.toResponse(address1)).thenReturn(response1);

        CustomerAddressResponse response2 = new CustomerAddressResponse();
        response2.setAddressId(20L);
        when(addressMapper.toResponse(address2)).thenReturn(response2);

        List<CustomerAddressResponse> results = addressService.getAddresses(1L);

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(10L, results.get(0).getAddressId());
        assertEquals(20L, results.get(1).getAddressId());
        verify(addressRepository, times(1)).findByCustomerCustomerId(1L);
    }

    @Test
    @DisplayName("Should return empty list when customer has no addresses")
    void getAddresses_Empty() {
        when(addressRepository.findByCustomerCustomerId(1L)).thenReturn(Collections.emptyList());

        List<CustomerAddressResponse> results = addressService.getAddresses(1L);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(addressRepository, times(1)).findByCustomerCustomerId(1L);
    }

    // =========================================================================
    // 2. createAddress Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw InvalidRequestException when user already has 5 addresses")
    void createAddress_ThrowsException_WhenLimitReached() {
        when(addressRepository.countByCustomerCustomerId(1L)).thenReturn(5L);

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> addressService.createAddress(1L, request));

        assertEquals("You can save up to 5 addresses.", ex.getMessage());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer is not found")
    void createAddress_ThrowsException_WhenCustomerNotFound() {
        when(addressRepository.countByCustomerCustomerId(1L)).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.createAddress(1L, request));

        assertEquals("Customer not found", ex.getMessage());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should automatically set first address as default")
    void createAddress_FirstAddress_AutomaticallyDefault() {
        request.setIsDefault(false); // Even if user requested false
        when(addressRepository.countByCustomerCustomerId(1L)).thenReturn(0L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressMapper.toResponse(any(CustomerAddress.class))).thenReturn(response1);

        CustomerAddressResponse result = addressService.createAddress(1L, request);

        assertNotNull(result);
        verify(addressRepository).save(argThat(CustomerAddress::getIsDefault));
        // clearExistingDefault is NOT called when addressCount == 0
        verify(addressRepository, never()).findByCustomerCustomerId(any());
    }

    @Test
    @DisplayName("Should clear existing default when subsequent address is marked default")
    void createAddress_SubsequentAddress_ClearsExistingDefault() {
        request.setIsDefault(true);
        when(addressRepository.countByCustomerCustomerId(1L)).thenReturn(1L);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(addressRepository.findByCustomerCustomerId(1L)).thenReturn(List.of(address1));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressMapper.toResponse(any(CustomerAddress.class))).thenReturn(response1);

        CustomerAddressResponse result = addressService.createAddress(1L, request);

        assertNotNull(result);
        // Verify previous default address was cleared
        assertFalse(address1.getIsDefault());
        verify(addressRepository, atLeastOnce()).save(address1);
    }

    // =========================================================================
    // 3. updateAddress Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw ResourceNotFoundException when address to update not found")
    void updateAddress_ThrowsException_WhenAddressNotFound() {
        when(addressRepository.findByAddressIdAndCustomerCustomerId(10L, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.updateAddress(1L, 10L, request));

        assertEquals("Address not found", ex.getMessage());
        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should clear existing default when updating non-default address to default")
    void updateAddress_SetsDefault_ClearsOthers() {
        address2.setIsDefault(false);
        request.setIsDefault(true);

        when(addressRepository.findByAddressIdAndCustomerCustomerId(20L, 1L)).thenReturn(Optional.of(address2));
        when(addressRepository.findByCustomerCustomerId(1L)).thenReturn(List.of(address1, address2));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressMapper.toResponse(any(CustomerAddress.class))).thenReturn(response1);

        addressService.updateAddress(1L, 20L, request);

        assertFalse(address1.getIsDefault());
        verify(addressRepository).save(address1);
    }

    @Test
    @DisplayName("Should not allow removing default status if currently default without setting another")
    void updateAddress_CannotUnsetDefault_ForcedToTrue() {
        address1.setIsDefault(true);
        request.setIsDefault(false); // Attempting to unset default

        when(addressRepository.findByAddressIdAndCustomerCustomerId(10L, 1L)).thenReturn(Optional.of(address1));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(addressMapper.toResponse(any(CustomerAddress.class))).thenReturn(response1);

        addressService.updateAddress(1L, 10L, request);

        // Verification: request.setIsDefault(true) is forced in service
        assertTrue(request.getIsDefault());
    }

    // =========================================================================
    // 4. deleteAddress Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw ResourceNotFoundException when address to delete not found")
    void deleteAddress_ThrowsException_WhenNotFound() {
        when(addressRepository.findByAddressIdAndCustomerCustomerId(10L, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.deleteAddress(1L, 10L));

        assertEquals("Address not found", ex.getMessage());
        verify(addressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should delete non-default address directly without reassigning default")
    void deleteAddress_NonDefault_Success() {
        address2.setIsDefault(false);
        when(addressRepository.findByAddressIdAndCustomerCustomerId(20L, 1L)).thenReturn(Optional.of(address2));

        addressService.deleteAddress(1L, 20L);

        verify(addressRepository, times(1)).delete(address2);
        verify(addressRepository, never()).findByCustomerCustomerId(anyLong());
    }

    @Test
    @DisplayName("Should delete default address and promote oldest remaining address as default")
    void deleteAddress_Default_PromotesOldestRemaining() {
        address1.setIsDefault(true);
        address2.setIsDefault(false);

        when(addressRepository.findByAddressIdAndCustomerCustomerId(10L, 1L)).thenReturn(Optional.of(address1));
        // Remaining list after delete
        when(addressRepository.findByCustomerCustomerId(1L)).thenReturn(new ArrayList<>(List.of(address1, address2)));

        addressService.deleteAddress(1L, 10L);

        verify(addressRepository).delete(address1);
        assertTrue(address2.getIsDefault());
        verify(addressRepository).save(address2);
    }

    // =========================================================================
    // 5. setDefaultAddress Tests
    // =========================================================================

    @Test
    @DisplayName("Should throw ResourceNotFoundException when setting default on non-existent address")
    void setDefaultAddress_ThrowsException_WhenNotFound() {
        when(addressRepository.findByAddressIdAndCustomerCustomerId(99L, 1L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> addressService.setDefaultAddress(1L, 99L));

        assertEquals("Address not found", ex.getMessage());
    }

    @Test
    @DisplayName("Should set address as default and clear existing default when not already default")
    void setDefaultAddress_Success() {
        address2.setIsDefault(false);
        when(addressRepository.findByAddressIdAndCustomerCustomerId(20L, 1L)).thenReturn(Optional.of(address2));
        when(addressRepository.findByCustomerCustomerId(1L)).thenReturn(List.of(address1, address2));
        when(addressRepository.save(address2)).thenReturn(address2);
        when(addressMapper.toResponse(address2)).thenReturn(response1);

        CustomerAddressResponse result = addressService.setDefaultAddress(1L, 20L);

        assertNotNull(result);
        assertTrue(address2.getIsDefault());
        assertFalse(address1.getIsDefault());
        verify(addressRepository).save(address1);
        verify(addressRepository).save(address2);
    }

    @Test
    @DisplayName("Should do nothing and return response if address is already default")
    void setDefaultAddress_AlreadyDefault() {
        address1.setIsDefault(true);
        when(addressRepository.findByAddressIdAndCustomerCustomerId(10L, 1L)).thenReturn(Optional.of(address1));
        when(addressMapper.toResponse(address1)).thenReturn(response1);

        CustomerAddressResponse result = addressService.setDefaultAddress(1L, 10L);

        assertNotNull(result);
        verify(addressRepository, never()).findByCustomerCustomerId(anyLong());
        verify(addressRepository, never()).save(any());
    }
}
