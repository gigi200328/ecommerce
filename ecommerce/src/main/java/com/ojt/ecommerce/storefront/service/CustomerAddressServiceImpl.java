package com.ojt.ecommerce.storefront.service;

import com.ojt.ecommerce.entity.Customer;
import com.ojt.ecommerce.entity.CustomerAddress;
import com.ojt.ecommerce.exception.InvalidRequestException;
import com.ojt.ecommerce.exception.ResourceNotFoundException;
import com.ojt.ecommerce.storefront.auth.repository.CustomerRepository;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressResponse;
import com.ojt.ecommerce.storefront.profile.mapper.CustomerAddressMapper;
import com.ojt.ecommerce.storefront.profile.repository.CustomerAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getAddresses(Long customerId) {
        return addressRepository.findByCustomerCustomerId(customerId).stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerAddressResponse createAddress(Long customerId, CustomerAddressRequest request) {
        long addressCount = addressRepository.countByCustomerCustomerId(customerId);
        if (addressCount >= 5) {
            throw new InvalidRequestException("You can save up to 5 addresses.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        boolean isFirstAddress = (addressCount == 0);
        boolean shouldBeDefault = isFirstAddress || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault && !isFirstAddress) {
            clearExistingDefault(customerId, null);
        }

        CustomerAddress address = new CustomerAddress();
        address.setCustomer(customer);
        address.setCreatedAt(LocalDateTime.now());
        addressMapper.updateEntityFromRequest(request, address);
        address.setIsDefault(shouldBeDefault);

        CustomerAddress saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(Long customerId, Long addressId, CustomerAddressRequest request) {
        CustomerAddress address = addressRepository.findByAddressIdAndCustomerCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        boolean isCurrentlyDefault = Boolean.TRUE.equals(address.getIsDefault());
        boolean requestedDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (requestedDefault && !isCurrentlyDefault) {
            clearExistingDefault(customerId, addressId);
        } else if (isCurrentlyDefault && !requestedDefault) {
            request.setIsDefault(true);
        }

        addressMapper.updateEntityFromRequest(request, address);
        address.setModifiedAt(LocalDateTime.now());

        CustomerAddress saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long customerId, Long addressId) {
        CustomerAddress address = addressRepository.findByAddressIdAndCustomerCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        
        if (wasDefault) {
            List<CustomerAddress> remaining = addressRepository.findByCustomerCustomerId(customerId);
            if (!remaining.isEmpty()) {
                CustomerAddress newDefault = remaining.stream()
                        .filter(a -> !a.getAddressId().equals(addressId))
                        .min(java.util.Comparator.comparing(CustomerAddress::getAddressId))
                        .orElse(null);
                if (newDefault != null) {
                    newDefault.setIsDefault(true);
                    newDefault.setModifiedAt(LocalDateTime.now());
                    addressRepository.save(newDefault);
                }
            }
        }
    }

    @Override
    @Transactional
    public CustomerAddressResponse setDefaultAddress(Long customerId, Long addressId) {
        CustomerAddress address = addressRepository.findByAddressIdAndCustomerCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        
        if (!Boolean.TRUE.equals(address.getIsDefault())) {
            clearExistingDefault(customerId, addressId);
            address.setIsDefault(true);
            address.setModifiedAt(LocalDateTime.now());
            addressRepository.save(address);
        }
        return addressMapper.toResponse(address);
    }

    private void clearExistingDefault(Long customerId, Long excludeAddressId) {
        List<CustomerAddress> addresses = addressRepository.findByCustomerCustomerId(customerId);
        for (CustomerAddress addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault()) && !addr.getAddressId().equals(excludeAddressId)) {
                addr.setIsDefault(false);
                addr.setModifiedAt(LocalDateTime.now());
                addressRepository.save(addr);
            }
        }
    }
}
