package com.ojt.ecommerce.storefront.auth.controller;
import com.ojt.ecommerce.storefront.security.CustomerPrincipal;
import com.ojt.ecommerce.storefront.auth.dto.*;
import com.ojt.ecommerce.storefront.auth.service.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/customer")
@RequiredArgsConstructor
public class CustomerAuthController {
    private final CustomerAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerMeResponse> getCurrentCustomer(@AuthenticationPrincipal CustomerPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentCustomer(principal.getCustomerId()));
    }
    
    @PutMapping("/me")
    public ResponseEntity<CustomerMeResponse> updateCustomerProfile(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ResponseEntity.ok(authService.updateCustomerProfile(principal.getCustomerId(), request));
    }
}
