package com.ojt.ecommerce.storefront.service;

import java.util.List;

import com.ojt.ecommerce.storefront.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.dto.CustomerAddressResponse;
public interface CustomerAddressService {
    List<CustomerAddressResponse> getAddresses(Long customerId);
    CustomerAddressResponse createAddress(Long customerId, CustomerAddressRequest request);
    CustomerAddressResponse updateAddress(Long customerId, Long addressId, CustomerAddressRequest request);
    void deleteAddress(Long customerId, Long addressId);
    CustomerAddressResponse setDefaultAddress(Long customerId, Long addressId);
}