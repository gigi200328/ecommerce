package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.AuthRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.AuthResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.ChangePasswordRequest;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.backofficeinventory.security.JwtUtil;
import com.ojt.ecommerce.backofficeinventory.service.StaffService;
import com.ojt.ecommerce.entity.Permission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backoffice/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StaffService staffService; 

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(userDetails);

        List<String> permissions = Collections.emptyList();
        if (userDetails.getUser().getUserRole() != null && userDetails.getUser().getUserRole().getPermissions() != null) {
            permissions = userDetails.getUser().getUserRole().getPermissions().stream()
                    .map(Permission::getPermissionName)
                    .toList();
        }

        AuthResponseDto response = AuthResponseDto.builder()
                .token(token)
                .username(userDetails.getUser().getUserName())
                .email(userDetails.getUser().getEmail()) 
                .role(userDetails.getUser().getUserRole().getRoleName())
                .permissions(permissions)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        try {
            staffService.changePassword(principal.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Password အောင်မြင်စွာ ပြောင်းလဲပြီးပါပြီ။"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
