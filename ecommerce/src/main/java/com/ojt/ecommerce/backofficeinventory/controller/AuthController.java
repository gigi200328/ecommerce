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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/backoffice/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final StaffService staffService; 
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService; 

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDto request) {
        // ၁။ Email ဖြင့် User အကောင့် ရှာဖွေခြင်း
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("field", "email", "message", "Email not registered."));
        }

        User user = userOpt.get();

        // ၂။ အကောင့် Status (Active/Inactive) စစ်ဆေးခြင်း
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("field", "email", "message", "Account is inactive. Please contact administrator."));
        }

        // ၃။ Password မှန်/မမှန် PasswordEncoder ဖြင့် တိုက်စစ်ခြင်း
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("field", "password", "message", "Incorrect password."));
        }

        // ၄။ CustomUserDetails တည်ဆောက်ပြီး Token ထုတ်ပေးခြင်း
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Login ဝင်လိုက်တိုင်း Token Version ကို ၁ တိုးမည် (Single device active session enforcement)
        user.setTokenVersion((user.getTokenVersion() == null ? 1 : user.getTokenVersion()) + 1);
        userRepository.save(user); 

        String token = jwtUtil.generateToken(userDetails);

        List<String> permissions = Collections.emptyList();
        if (user.getUserRole() != null && user.getUserRole().getPermissions() != null) {
            permissions = user.getUserRole().getPermissions().stream()
                    .map(Permission::getPermissionName)
                    .toList();
        }

        // Login အောင်မြင်ကြောင်း Audit Log သို့ မှတ်တမ်းတင်ခြင်း
        try {
            auditLogService.logAuthAction(
                AuditAction.LOGIN,
                "Staff/Admin logged into the system: " + user.getUserName(),
                user.getUserId(),
                user.getUserName(),
                "SUCCESS"
            );
        } catch (Exception e) {
            System.err.println("Audit Log Failed for Login: " + e.getMessage());
        }

        AuthResponseDto response = AuthResponseDto.builder()
                .token(token)
                .username(user.getUserName())
                .email(user.getEmail()) 
                .role(user.getUserRole() != null ? user.getUserRole().getRoleName() : "STAFF")
                .permissions(permissions) 
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
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

    /**
     * Logout ပြုလုပ်သည့်အခါ Server ပေါ်ရှိ User ၏ Token Version ကို ၁ တိုးပေးခြင်းဖြင့်
     * လက်ရှိ JWT Token ကို ချက်ချင်း ပယ်ဖျက် (Revoke) သည့် စနစ်
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Principal principal) {
        try {
            if (principal != null) {
                userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                    // Token Version ကို ၁ တိုး၍ လက်ရှိ Token ကို Server ပေါ်တွင် ချက်ချင်း အသုံးမဝင်တော့အောင် ပြုလုပ်မည်
                    user.setTokenVersion((user.getTokenVersion() == null ? 1 : user.getTokenVersion()) + 1);
                    userRepository.save(user);

                    auditLogService.logAuthAction(
                        AuditAction.LOGOUT,
                        "Staff/Admin logged out of the system: " + user.getUserName(),
                        user.getUserId(),
                        user.getUserName(),
                        "SUCCESS"
                    );
                });
            }
        } catch (Exception e) {
            System.err.println("Audit Log Failed for Logout: " + e.getMessage());
        }
        
        return ResponseEntity.ok(Map.of("message", "Successfully logged out."));
    }
}
