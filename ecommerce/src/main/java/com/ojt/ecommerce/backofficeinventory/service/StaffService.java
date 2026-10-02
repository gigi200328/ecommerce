package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.ChangePasswordRequest; // <--- အသစ်ထည့်ထားသော Import
import com.ojt.ecommerce.backofficeinventory.dto.StaffRequestDTO;
import com.ojt.ecommerce.backofficeinventory.dto.StaffResponseDTO;
import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
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
            throw new RuntimeException("ဤအီးမေးလ်ဖြင့် အကောင့်ဖွင့်ပြီးသား ဖြစ်နေပါသည်။");
        }

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role မတွေ့ရှိပါ။"));

        User newStaff = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // Password ကို Hash လုပ်ခြင်း
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .userRole(role)
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
                        // Database တွင် Role မရှိခဲ့လျှင် Error မတက်စေရန် null စစ်ပေးထားပါသည်
                        .roleName(staff.getUserRole() != null ? staff.getUserRole().getRoleName() : "N/A") 
                        .status(staff.getStatus())
                        .createdAt(staff.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // Staff အချက်အလက် ပြင်ဆင်ရန် (PUT)
    public StaffResponseDTO updateStaff(Long id, StaffRequestDTO request) {
        User existingStaff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff မတွေ့ရှိပါ။ ID: " + id));

        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role မတွေ့ရှိပါ။"));

        existingStaff.setUserName(request.getUserName());
        existingStaff.setUserRole(role);
        
        if (!existingStaff.getEmail().equals(request.getEmail()) && 
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("ဤအီးမေးလ်သည် အသုံးပြုပြီးသား ဖြစ်နေပါသည်။");
        }
        existingStaff.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            existingStaff.setPasswordHash(passwordEncoder.encode(request.getPassword()));
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
                .orElseThrow(() -> new RuntimeException("Staff မတွေ့ရှိပါ။ ID: " + id));
        
        existingStaff.setStatus("INACTIVE"); // အပြီးမဖျက်ဘဲ Inactive သာ လုပ်လိုက်သည်
        existingStaff.setModifiedAt(LocalDateTime.now());
        
        userRepository.save(existingStaff);
    }
    
    // ==========================================
    // အသစ်ထပ်ထည့်ထားသော Password ပြောင်းရန် Method
    // ==========================================
    public void changePassword(String email, ChangePasswordRequest request) {
        // ၁။ Login ဝင်ထားသော User ကို Email ဖြင့် Database တွင် ရှာခြင်း
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("အကောင့် ရှာမတွေ့ပါ။"));

        // ၂။ Current Password မှန်/မမှန် စစ်ဆေးခြင်း
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("လက်ရှိ Password မှားယွင်းနေပါသည်။");
        }

        // ၃။ Password အသစ်ကို Hash ပြုလုပ်၍ သိမ်းဆည်းခြင်း
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setModifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }
 // Staff ၏ Status (ACTIVE / INACTIVE) ကို ပြောင်းလဲရန် Method
    public void updateStaffStatus(Long id, String status) {
        User existingStaff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff မတွေ့ရှိပါ။ ID: " + id));
        
        existingStaff.setStatus(status);
        existingStaff.setModifiedAt(LocalDateTime.now());
        
        userRepository.save(existingStaff);
    }
}