package com.ojt.ecommerce.storefront.profile.service;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressResponse;
import java.util.List;
public interface CustomerAddressService {
    List<CustomerAddressResponse> getAddresses(Long customerId);
    CustomerAddressResponse createAddress(Long customerId, CustomerAddressRequest request);
    CustomerAddressResponse updateAddress(Long customerId, Long addressId, CustomerAddressRequest request);
    void deleteAddress(Long customerId, Long addressId);
    CustomerAddressResponse setDefaultAddress(Long customerId, Long addressId);
}