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
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("This email address is already registered.");
        }

        // Only the Primary Super Admin can create accounts with the ADMIN role
        if ("ADMIN".equalsIgnoreCase(request.getRoleName())) {
            User currentUser = getCurrentUser();
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator can appoint or create Administrator accounts.");
            }
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found: " + request.getRoleName()));

        User newStaff = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
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

        // 1. Super Admin Protection: Other admins cannot modify the primary Super Admin
        if (isSuperAdmin(existingStaff)) {
            if (currentUser == null || !isSuperAdmin(currentUser)) {
                throw new RuntimeException("The primary Super Administrator account cannot be modified by other administrators.");
            }
            if (!"ADMIN".equalsIgnoreCase(request.getRoleName())) {
                throw new RuntimeException("The primary Super Administrator role cannot be demoted.");
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

        existingStaff.setUserName(request.getUserName());
        existingStaff.setUserRole(role);
        
        if (!existingStaff.getEmail().equals(request.getEmail()) && 
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("This email address is already in use.");
        }
        existingStaff.setEmail(request.getEmail());

        // If password is reset by admin, require user to change it on their next login
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            existingStaff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            existingStaff.setMustChangePassword(true);
            existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        }

        existingStaff.setModifiedAt(LocalDateTime.now());
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

        existingStaff.setStatus("INACTIVE");
        existingStaff.setModifiedAt(LocalDateTime.now());
        existingStaff.setTokenVersion((existingStaff.getTokenVersion() == null ? 1 : existingStaff.getTokenVersion()) + 1);
        
        userRepository.save(existingStaff);
    }
    
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Account not found with email: " + email));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false); // cleared upon successful password change!
        user.setModifiedAt(LocalDateTime.now());
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

        existingStaff.setStatus(status.toUpperCase());
        existingStaff.setModifiedAt(LocalDateTime.now());
        
        userRepository.save(existingStaff);
    }

    @Transactional(readOnly = true)
    public StaffResponseDTO getStaffById(Long id) {
        User staff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff member not found with ID: " + id));

        return StaffResponseDTO.builder()
                .userId(staff.getUserId())
                .userName(staff.getUserName())
                .email(staff.getEmail())
                .roleName(staff.getUserRole() != null ? staff.getUserRole().getRoleName() : "N/A")
                .status(staff.getStatus())
                .createdAt(staff.getCreatedAt())
                .build();
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

        // 3. Regular admins cannot suspend fellow admins
        if (targetStaff.getUserRole() != null && "ADMIN".equalsIgnoreCase(targetStaff.getUserRole().getRoleName())) {
            if (currentUser != null && !isSuperAdmin(currentUser)) {
                throw new RuntimeException("Only the primary Super Administrator can suspend another administrator.");
            }

            long activeAdminCount = userRepository.countByRoleNameAndStatus("ADMIN", "ACTIVE");
            if (activeAdminCount <= 1) {
                throw new RuntimeException("Cannot suspend the last remaining active Administrator.");
            }
        }
    }

    private boolean isSuperAdmin(User user) {
        if (user == null) return false;
        return "admin@ecommerce.com".equalsIgnoreCase(user.getEmail()) || (user.getUserId() != null && user.getUserId() == 1L);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUser();
        }
        return null;
    }
}
