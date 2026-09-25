package com.ojt.ecommerce.storefront.controller;

import com.ojt.ecommerce.security.CustomerPrincipal;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressRequest;
import com.ojt.ecommerce.storefront.profile.dto.CustomerAddressResponse;
import com.ojt.ecommerce.storefront.profile.service.CustomerAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile/addresses")
@RequiredArgsConstructor
public class CustomerAddressController {

    private final CustomerAddressService addressService;

    @GetMapping
    public ResponseEntity<List<CustomerAddressResponse>> getAddresses(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(addressService.getAddresses(principal.getCustomerId()));
    }

    @PostMapping
    public ResponseEntity<CustomerAddressResponse> createAddress(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(addressService.createAddress(principal.getCustomerId(), request));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<CustomerAddressResponse> updateAddress(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long addressId,
            @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(principal.getCustomerId(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long addressId) {
        addressService.deleteAddress(principal.getCustomerId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<CustomerAddressResponse> setDefaultAddressPatch(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(principal.getCustomerId(), addressId));
    }

    @PutMapping("/{addressId}/default")
    public ResponseEntity<CustomerAddressResponse> setDefaultAddressPut(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @PathVariable Long addressId) {
        return ResponseEntity.ok(addressService.setDefaultAddress(principal.getCustomerId(), addressId));
    }
}
