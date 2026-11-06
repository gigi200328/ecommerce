package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.ChangePasswordRequest;
import com.ojt.ecommerce.backofficeinventory.dto.StaffRequestDTO;
import com.ojt.ecommerce.backofficeinventory.dto.StaffResponseDTO;
import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffResponseDTO createStaff(StaffRequestDTO request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email address is required.");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Email syntax validation
        if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("Invalid email format.");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new RuntimeException("This email address is already registered.");
        }

        String normalizedUserName = request.getUserName() != null ? request.getUserName().trim() : "";
        if (normalizedUserName.isEmpty()) {
            throw new RuntimeException("Full name is required.");
        }

        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;

        // Only the Primary Super Admin can create accounts with the ADMIN role
        if ("ADMIN".equalsIgnoreCase(request.getRoleName())) {
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator can appoint or create Administrator accounts.");
            }
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        String password = request.getPassword() != null ? request.getPassword().trim() : "";
        // Validate password complexity rules
        validatePasswordStrength(password);

        User newStaff = User.builder()
                .userName(normalizedUserName)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(password))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .createdBy(currentUserId)
                .userRole(role)
                .tokenVersion(1)
                .mustChangePassword(true) // newly created staff/admin accounts must change password on first login
                .build();

        User savedStaff = userRepository.save(newStaff);

        return StaffResponseDTO.builder()
                .userId(savedStaff.getUserId())
                .userName(savedStaff.getUserName())
                .email(savedStaff.getEmail())
                .roleName(savedStaff.getUserRole().getRoleName())
                .status(savedStaff.getStatus())
                .createdAt(savedStaff.getCreatedAt())
                .build();
    }
    
    @Transactional(readOnly = true) 
    public List<StaffResponseDTO> getAllStaff() {
        return userRepository.findAll().stream()
                .map(staff -> StaffResponseDTO.builder()
                        .userId(staff.getUserId())
                        .userName(staff.getUserName())
                        .email(staff.getEmail())
                        .roleName(staff.getUserRole() != null ? staff.getUserRole().getRoleName() : "N/A") 
                        .status(staff.getStatus())
                        .createdAt(staff.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public StaffResponseDTO updateStaff(Long id, StaffRequestDTO request) {
        User existingStaff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff member not found with ID: " + id));

        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;

        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new RuntimeException("Email address is required.");
        }
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (!normalizedEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new RuntimeException("Invalid email format.");
        }

        String normalizedUserName = request.getUserName() != null ? request.getUserName().trim() : "";
        if (normalizedUserName.isEmpty()) {
            throw new RuntimeException("Full name is required.");
        }

        // 1. Super Admin Protection: Other admins cannot modify the primary Super Admin
        if (isSuperAdmin(existingStaff)) {
            if (currentUser == null || !isSuperAdmin(currentUser)) {
                throw new RuntimeException("The primary Super Administrator account cannot be modified by other administrators.");
            }
            if (!"ADMIN".equalsIgnoreCase(request.getRoleName())) {
                throw new RuntimeException("The primary Super Administrator role cannot be demoted.");
            }
            if (!existingStaff.getEmail().equalsIgnoreCase(normalizedEmail)) {
                throw new RuntimeException("The primary Super Administrator root email address cannot be changed.");
            }
        }

        // 2. Peer Admin Protection: Regular admins cannot modify other Admin accounts
        if (existingStaff.getUserRole() != null && "ADMIN".equalsIgnoreCase(existingStaff.getUserRole().getRoleName())) {
            if (currentUser != null && !isSuperAdmin(currentUser) && !currentUser.getUserId().equals(existingStaff.getUserId())) {
                throw new RuntimeException("Subordinate administrators cannot modify or reassign roles of other administrators.");
            }
        }

        // 3. Admin Promotion Protection: Only Super Admin can promote someone to ADMIN
        if (!"ADMIN".equalsIgnoreCase(existingStaff.getUserRole() != null ? existingStaff.getUserRole().getRoleName() : "")
                && "ADMIN".equalsIgnoreCase(request.getRoleName())) {
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator can promote a staff member to Administrator.");
            }
        }

        // 4. Role Reassignment Protection: Only Super Admin can change any account's role
        boolean isRoleChanging = existingStaff.getUserRole() == null || 
                                 !existingStaff.getUserRole().getRoleName().equalsIgnoreCase(request.getRoleName());
        if (isRoleChanging) {
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator has the authority to change or reassign account roles.");
            }
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        // If demoting an Admin, ensure they are not the last remaining active Admin
        if (existingStaff.getUserRole() != null && "ADMIN".equalsIgnoreCase(existingStaff.getUserRole().getRoleName())
                && !"ADMIN".equalsIgnoreCase(request.getRoleName())) {
            validateDeactivationAllowed(existingStaff);
        }

        // If role changes, invalidate their existing session token
        if (existingStaff.getUserRole() == null || !existingStaff.getUserRole().getRoleName().equalsIgnoreCase(request.getRoleName())) {
            existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        }

        existingStaff.setUserName(normalizedUserName);
        existingStaff.setUserRole(role);
        
        // 5. Email Update Policy (Option 2): Only Super Admin can update an account's email address
        if (!existingStaff.getEmail().equalsIgnoreCase(normalizedEmail)) {
            if (currentUser == null || !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator has the authority to change or update staff email addresses.");
            }
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                throw new RuntimeException("This email address is already in use.");
            }
            existingStaff.setEmail(normalizedEmail);
            // Invalidate session if email changes so they must re-authenticate with new email
            existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        }

        // If password is reset by admin, require user to change it on their next login
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            if (isSuperAdmin(existingStaff)) {
                throw new RuntimeException("Super Administrator password cannot be reset from this staff management form. Please use the Change Password settings in Profile.");
            }
            String newPassword = request.getPassword().trim();
            validatePasswordStrength(newPassword);
            existingStaff.setPasswordHash(passwordEncoder.encode(newPassword));
            existingStaff.setMustChangePassword(true);
            existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        }

        existingStaff.setModifiedAt(LocalDateTime.now());
        existingStaff.setModifiedBy(currentUserId);
        User updatedStaff = userRepository.save(existingStaff);

        return StaffResponseDTO.builder()
                .userId(updatedStaff.getUserId())
                .userName(updatedStaff.getUserName())
                .email(updatedStaff.getEmail())
                .roleName(updatedStaff.getUserRole().getRoleName())
                .status(updatedStaff.getStatus())
                .createdAt(updatedStaff.getCreatedAt())
                .build();
    }

    public void deleteStaff(Long id) {
        User existingStaff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff member not found with ID: " + id));
        
        validateDeactivationAllowed(existingStaff);

        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;

        existingStaff.setStatus("INACTIVE");
        existingStaff.setModifiedAt(LocalDateTime.now());
        existingStaff.setModifiedBy(currentUserId);
        existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        
        userRepository.save(existingStaff);
    }
    
    public void changePassword(String email, ChangePasswordRequest request) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Account not found with email: " + normalizedEmail));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect.");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new RuntimeException("New password must be different from current password.");
        }

        validatePasswordStrength(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false); // cleared upon successful password change!
        user.setModifiedAt(LocalDateTime.now());
        user.setModifiedBy(user.getUserId());
        user.setTokenVersion((user.getTokenVersion() == null ? 1 : user.getTokenVersion()) + 1);
        userRepository.save(user);
    }

    public void updateStaffStatus(Long id, String status) {
        User existingStaff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff member not found with ID: " + id));
        
        if ("INACTIVE".equalsIgnoreCase(status)) {
            validateDeactivationAllowed(existingStaff);
            existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        }

        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;

        existingStaff.setStatus(status.toUpperCase());
        existingStaff.setModifiedAt(LocalDateTime.now());
        existingStaff.setModifiedBy(currentUserId);
        
        userRepository.save(existingStaff);
    }

    @Transactional(readOnly = true)
    public StaffResponseDTO getStaffById(Long id) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff member not found with ID: " + id));

        // If target is Super Admin, only Super Admin can view or edit details
        if (isSuperAdmin(staff)) {
            User currentUser = getCurrentUser();
            if (currentUser == null || !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Access denied: You do not have permission to view or edit the Super Administrator account.");
            }
        }

        return StaffResponseDTO.builder()
                .userId(staff.getUserId())
                .userName(staff.getUserName())
                .email(staff.getEmail())
                .roleName(staff.getUserRole() != null ? staff.getUserRole().getRoleName() : "N/A")
                .status(staff.getStatus())
                .createdAt(staff.getCreatedAt())
                .build();
    }

    /**
     * Password Complexity Policy
     * အနည်းဆုံး ၈ လုံး၊ စာလုံးကြီး (A-Z)၊ စာလုံးသေး (a-z)၊ ဂဏန်း (0-9) နှင့် သင်္ကေတ (!@#$%^&*) မဖြစ်မနေ ပါရှိရမည်
     */
    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Password must be at least 8 characters long.");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Password must contain at least one uppercase letter (A-Z).");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Password must contain at least one lowercase letter (a-z).");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("Password must contain at least one number (0-9).");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new RuntimeException("Password must contain at least one special character (e.g. !@#$%^&*).");
        }
    }

    private void validateDeactivationAllowed(User targetStaff) {
        // 1. Super Admin can never be suspended or deactivated
        if (isSuperAdmin(targetStaff)) {
            throw new RuntimeException("The primary Super Administrator account cannot be suspended or deactivated.");
        }

        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;

        // 2. Cannot suspend self
        if (currentUserId != null && currentUserId.equals(targetStaff.getUserId())) {
            throw new RuntimeException("You cannot deactivate or suspend your own account.");
        }

        // 3. Regular admins cannot suspend fellow admins (Only Super Admin can suspend another administrator)
        if (targetStaff.getUserRole() != null && "ADMIN".equalsIgnoreCase(targetStaff.getUserRole().getRoleName())) {
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator can suspend another administrator.");
            }
        }
    }

    private boolean isSuperAdmin(User user) {
        if (user == null) return false;
        return "admin@ecommerce.com".equalsIgnoreCase(user.getEmail()) || (user.getUserId() != null && user.getUserId() == 1L);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getUser();
            }
            if (auth.getName() != null) {
                return userRepository.findByEmail(auth.getName().trim().toLowerCase()).orElse(null);
            }
        }
        return null;
    }
}
