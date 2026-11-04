package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.AuthRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.AuthResponseDto;
import com.ojt.ecommerce.backofficeinventory.dto.ChangePasswordRequest;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.backofficeinventory.security.JwtUtil;
import com.ojt.ecommerce.backofficeinventory.service.LoginAttemptService;
import com.ojt.ecommerce.backofficeinventory.service.StaffService;
import com.ojt.ecommerce.entity.Permission; 
import com.ojt.ecommerce.entity.User;       

import com.ojt.ecommerce.annotation.Auditable;
import com.ojt.ecommerce.entity.AuditAction;
import com.ojt.ecommerce.backofficeinventory.service.AuditLogService; 

import jakarta.servlet.http.HttpServletRequest;
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
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDto request, HttpServletRequest httpRequest) {
        String clientIp = loginAttemptService.getClientIp(httpRequest);

        // ၁။ IP Block ခံထားရခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း (Brute-Force Lockout check)
        if (loginAttemptService.isBlocked(clientIp)) {
            long remainingMins = loginAttemptService.getRemainingLockMinutes(clientIp);
            String errorMsg = String.format("Password (၅) ကြိမ် ဆက်တိုက် မှားယွင်းထားသဖြင့် အကောင့်လုံခြုံရေးအရ %d မိနစ်ကြာ Login ဝင်ခွင့်ကို ယာယီဆိုင်းငံ့ထားပါသည်။ ခေတ္တစောင့်ဆိုင်းပြီးမှ ပြန်လည်ကြိုးစားပါ။", remainingMins);

            auditLogService.logAuthAction(
                AuditAction.LOGIN,
                "[SECURITY ALERT] Blocked login attempt from IP " + clientIp + " (Lock active: " + remainingMins + " mins left)",
                null,
                request.getEmail(),
                "FAILED (BLOCKED)"
            );

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("field", "password", "message", errorMsg, "blocked", true, "remainingMinutes", remainingMins));
        }

        // ၂။ Email ဖြင့် User အကောင့် ရှာဖွေခြင်း
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            int remainingAttempts = loginAttemptService.loginFailed(clientIp);
            String errorMsg = "Email not registered.";

            auditLogService.logAuthAction(
                AuditAction.LOGIN,
                "Failed login attempt (Unregistered email: " + request.getEmail() + ") from IP " + clientIp,
                null,
                request.getEmail(),
                "FAILED"
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("field", "email", "message", errorMsg));
        }

        User user = userOpt.get();

        // ၃။ အကောင့် Status (Active/Inactive) စစ်ဆေးခြင်း
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("field", "email", "message", "Account is inactive. Please contact administrator."));
        }

        // ၄။ Password မှန်/မမှန် PasswordEncoder ဖြင့် တိုက်စစ်ခြင်း
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int remainingAttempts = loginAttemptService.loginFailed(clientIp);

            if (remainingAttempts <= 0) {
                long lockMins = loginAttemptService.getRemainingLockMinutes(clientIp);
                String blockMsg = String.format("Password (၅) ကြိမ် ဆက်တိုက် မှားယွင်းထားသဖြင့် အကောင့်လုံခြုံရေးအရ %d မိနစ်ကြာ Login ဝင်ခွင့်ကို ယာယီဆိုင်းငံ့ထားပါသည်။ ခေတ္တစောင့်ဆိုင်းပြီးမှ ပြန်လည်ကြိုးစားပါ။", lockMins);

                auditLogService.logAuthAction(
                    AuditAction.LOGIN,
                    "[SECURITY ALERT] IP " + clientIp + " temporarily blocked for 15 mins (5 consecutive failed password attempts for user: " + user.getUserName() + ")",
                    user.getUserId(),
                    user.getUserName(),
                    "FAILED (BLOCKED)"
                );

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("field", "password", "message", blockMsg, "blocked", true, "remainingMinutes", lockMins));
            }

            String errorMsg = String.format("Password မှားယွင်းနေပါသည်။ (နောက်ထပ် %d ကြိမ်သာ ရိုက်ခွင့်ရှိပါသည်)", remainingAttempts);

            auditLogService.logAuthAction(
                AuditAction.LOGIN,
                "Failed login attempt (Incorrect password, " + remainingAttempts + " attempts left) for user: " + user.getUserName(),
                user.getUserId(),
                user.getUserName(),
                "FAILED"
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("field", "password", "message", errorMsg, "remainingAttempts", remainingAttempts));
        }

        // ၅။ Password မှန်ကန်ပါက မှားထားသော အကြိမ်ရေများ Reset လုပ်မည်
        loginAttemptService.loginSucceeded(clientIp);

        // ၆။ CustomUserDetails တည်ဆောက်ပြီး Token ထုတ်ပေးခြင်း
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
