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
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        // Super Admin (admin@ecommerce.com) is immune to accidental lockout from other accounts
        boolean isSuperAdmin = "admin@ecommerce.com".equalsIgnoreCase(email);

        // 1. Check account lockout status by Email and IP
        if (!isSuperAdmin && loginAttemptService.isBlocked(email, clientIp)) {
            long remainingMins = loginAttemptService.getRemainingLockMinutes(email, clientIp);
            String errorMsg = String.format("Too many consecutive failed login attempts. For security reasons, login is temporarily suspended for %d minutes. Please try again later.", remainingMins);

            auditLogService.logAuthAction(
                AuditAction.LOGIN,
                "[SECURITY ALERT] Blocked login attempt for " + email + " from IP " + clientIp + " (Lock active: " + remainingMins + " mins left)",
                null,
                email,
                "FAILED (BLOCKED)"
            );

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("field", "password", "message", errorMsg, "blocked", true, "remainingMinutes", remainingMins));
        }

        // 2. Find user account by Email (normalized)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            int remainingAttempts = loginAttemptService.loginFailed(email, clientIp);
            String errorMsg = "Email not registered.";

            auditLogService.logAuthAction(
                AuditAction.LOGIN,
                "Failed login attempt (Unregistered email: " + email + ") from IP " + clientIp,
                null,
                email,
                "FAILED"
            );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("field", "email", "message", errorMsg));
        }

        User user = userOpt.get();

        // 3. Check account active status
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("field", "email", "message", "Account is inactive. Please contact administrator."));
        }

        // 4. Verify password with PasswordEncoder
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int remainingAttempts = loginAttemptService.loginFailed(email, clientIp);

            if (remainingAttempts <= 0) {
                long lockMins = loginAttemptService.getRemainingLockMinutes(email, clientIp);
                String blockMsg = String.format("Too many consecutive failed login attempts. For security reasons, login is temporarily suspended for %d minutes. Please try again later.", lockMins);

                auditLogService.logAuthAction(
                    AuditAction.LOGIN,
                    "[SECURITY ALERT] Account " + email + " from IP " + clientIp + " temporarily blocked for 15 mins (5 consecutive failed password attempts for user: " + user.getUserName() + ")",
                    user.getUserId(),
                    user.getUserName(),
                    "FAILED (BLOCKED)"
                );

                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("field", "password", "message", blockMsg, "blocked", true, "remainingMinutes", lockMins));
            }

            String errorMsg = String.format("Incorrect password. (%d attempts remaining)", remainingAttempts);

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

        // 5. Successful password match - reset failure counters
        loginAttemptService.loginSucceeded(email, clientIp);

        // 6. Build UserDetails and generate Token
        CustomUserDetails userDetails = new CustomUserDetails(user);

        // Enforce single active session per user by incrementing token version
        user.setTokenVersion((user.getTokenVersion() == null ? 1 : user.getTokenVersion()) + 1);
        userRepository.save(user); 

        String token = jwtUtil.generateToken(userDetails);

        List<String> permissions = Collections.emptyList();
        if (user.getUserRole() != null && user.getUserRole().getPermissions() != null) {
            permissions = user.getUserRole().getPermissions().stream()
                    .map(Permission::getPermissionName)
                    .toList();
        }

        // Record successful login audit log
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

    /**
     * Endpoint for authenticated user to update their own profile name
     */
    @Auditable(action = AuditAction.UPDATE, moduleName = "Authentication")
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String newUserName = request.get("userName");
        if (newUserName == null || newUserName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Full name cannot be blank."));
        }
        String trimmedName = newUserName.trim();
        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            return ResponseEntity.badRequest().body(Map.of("message", "Full name must be between 2 and 50 characters."));
        }

        String email = principal.getName() != null ? principal.getName().trim().toLowerCase() : "";
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User account not found."));
        }

        User user = userOpt.get();
        String oldName = user.getUserName();
        user.setUserName(trimmedName);
        user.setModifiedAt(java.time.LocalDateTime.now());
        user.setModifiedBy(user.getUserId());
        userRepository.save(user);

        try {
            auditLogService.logAuthAction(
                AuditAction.UPDATE,
                "User updated own profile full name from '" + oldName + "' to '" + user.getUserName() + "'",
                user.getUserId(),
                user.getUserName(),
                "SUCCESS"
            );
        } catch (Exception e) {
            System.err.println("Audit Log Failed for Profile Update: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
            "message", "Profile updated successfully.",
            "userName", user.getUserName(),
            "email", user.getEmail()
        ));
    }

    @Auditable(action = AuditAction.UPDATE, moduleName = "Authentication")
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        try {
            staffService.changePassword(principal.getName(), request);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Logout endpoint to immediately revoke session
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(Principal principal) {
        try {
            if (principal != null) {
                userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                    user.setTokenVersion((user.getTokenVersion() == null ? 1 : user.getTokenVersion()) + 1);
                    userRepository.save(user);

                    auditLogService.logAuthAction(
                        AuditAction.LOGIN,
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

    /**
     * Lightweight session validation endpoint
     */
    @GetMapping("/validate-session")
    public ResponseEntity<?> validateSession(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "No active session found."));
        }
        return ResponseEntity.ok(Map.of("valid", true, "username", principal.getName()));
    }

    /**
     * Emergency lockout reset endpoint
     */
    @GetMapping("/reset-lockout")
    public ResponseEntity<?> resetLockout() {
        loginAttemptService.resetAll();
        return ResponseEntity.ok(Map.of("message", "Lockout cache successfully cleared."));
    }
}
