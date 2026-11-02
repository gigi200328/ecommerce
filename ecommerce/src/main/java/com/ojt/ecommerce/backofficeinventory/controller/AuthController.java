package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.AuthRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.AuthResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.ChangePasswordRequest;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.backofficeinventory.security.JwtUtil;
import com.ojt.ecommerce.backofficeinventory.service.StaffService;
import com.ojt.ecommerce.entity.Permission; 
import com.ojt.ecommerce.entity.User;       

import com.ojt.ecommerce.annotation.Auditable;
import com.ojt.ecommerce.entity.AuditAction;
import com.ojt.ecommerce.backofficeinventory.service.AuditLogService; 

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
    private final UserRepository userRepository;
    
    private final AuditLogService auditLogService; 

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        // --- Login ဝင်လိုက်တိုင်း Token Version ကို ၁ တိုးမည် ---
        user.setTokenVersion((user.getTokenVersion() == null ? 1 : user.getTokenVersion()) + 1);
        userRepository.save(user); 

        String token = jwtUtil.generateToken(userDetails);

        List<String> permissions = Collections.emptyList();
        if (user.getUserRole() != null && user.getUserRole().getPermissions() != null) {
            permissions = user.getUserRole().getPermissions().stream()
                    .map(Permission::getPermissionName)
                    .toList();
        }

        // -------------------------------------------------------------
        // Login အောင်မြင်ကြောင်း Audit Log သို့ Manual မှတ်တမ်းတင်ခြင်း
        // -------------------------------------------------------------
        try {
            auditLogService.logAction(
                AuditAction.LOGIN,                        // 1. AuditAction 
                "Authentication",                         // 2. Module Name 
                "Staff/Admin logged into the system",     // 3. Action/Description 
                null,                                     // 4. Entity ID (မလိုအပ်ပါ)
                null,                                     // 5. Old Value (မလိုအပ်ပါ)
                null,                                     // 6. New Value (မလိုအပ်ပါ)
                user.getUserName(),                       // 7. Performed By 
                null                                      // 8. IP Address (မလိုအပ်ပါ)
            );
        } catch (Exception e) {
            System.err.println("Audit Log Failed for Login: " + e.getMessage());
        }

        AuthResponseDto response = AuthResponseDto.builder()
                .token(token)
                .username(user.getUserName())
                .email(user.getEmail()) 
                .role(user.getUserRole().getRoleName())
                .permissions(permissions) 
                .build();

        return ResponseEntity.ok(response);
    }

    @Auditable(action = AuditAction.UPDATE, moduleName = "Authentication")
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        try {
            staffService.changePassword(principal.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Password အောင်မြင်စွာ ပြောင်းလဲပြီးပါပြီ။"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(Principal principal) {
        try {
            if (principal != null) {
                auditLogService.logAction(
                    AuditAction.LOGOUT,                       // 1. AuditAction 
                    "Authentication",                         // 2. Module Name
                    "Staff/Admin logged out of the system",   // 3. Action/Description
                    null,                                     // 4. Entity ID
                    null,                                     // 5. Old Value
                    null,                                     // 6. New Value
                    principal.getName(),                      // 7. Performed By 
                    null                                      // 8. IP Address
                );
            }
        } catch (Exception e) {
            System.err.println("Audit Log Failed for Logout: " + e.getMessage());
        }
        
        return ResponseEntity.ok(Map.of("message", "အောင်မြင်စွာ Logout ပြုလုပ်ပြီးပါပြီ။"));
    }
}