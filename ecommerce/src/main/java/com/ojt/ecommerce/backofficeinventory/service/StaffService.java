package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.dto.StaffRequestDTO;
import com.ojt.ecommerce.backofficeinventory.dto.StaffResponseDTO;
import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public StaffResponseDTO createStaff(StaffRequestDTO request) {
        
        // ၁။ အီးမေးလ် အသုံးပြုပြီးသား ရှိ/မရှိ စစ်ဆေးခြင်း
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("ဤအီးမေးလ်ဖြင့် အကောင့်ဖွင့်ပြီးသား ဖြစ်နေပါသည်။");
        }

        // ၂။ Request မှ ပေးလိုက်သော Role Name ဖြင့် Database ထဲတွင် ရှာဖွေခြင်း
        Role role = roleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role မတွေ့ရှိပါ။"));

        // ၃။ Staff အသစ် (User Entity) တည်ဆောက်ခြင်း
        User newStaff = User.builder()
                .userName(request.getUserName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword())) // Password ကို Hash လုပ်ခြင်း
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .userRole(role)
                .build();

        // ၄။ Database သို့ သိမ်းဆည်းခြင်း
        User savedStaff = userRepository.save(newStaff);

        // ၅. Frontend သို့ ပြန်ပို့ရန် DTO သို့ ပြောင်းလဲခြင်း
        return StaffResponseDTO.builder()
                .userId(savedStaff.getUserId())
                .userName(savedStaff.getUserName())
                .email(savedStaff.getEmail())
                .roleName(savedStaff.getUserRole().getRoleName())
                .status(savedStaff.getStatus())
                .createdAt(savedStaff.getCreatedAt())
                .build();
    }
    
    public List<StaffResponseDTO> getAllStaff() {
        return userRepository.findAll().stream()
                .map(staff -> StaffResponseDTO.builder()
                        .userId(staff.getUserId())
                        .userName(staff.getUserName())
                        .email(staff.getEmail())
                        .roleName(staff.getUserRole().getRoleName())
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
        
        // Email အသစ်ပြောင်းမယ်ဆိုရင် တခြားသူ သုံးပြီးသားလား စစ်ဆေးရန်
        if (!existingStaff.getEmail().equals(request.getEmail()) && 
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("ဤအီးမေးလ်သည် အသုံးပြုပြီးသား ဖြစ်နေပါသည်။");
        }
        existingStaff.setEmail(request.getEmail());

        // Password အသစ်ပါလာရင်သာ ပြောင်းပေးရန်
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

    // Staff အကောင့် ပိတ်ရန် - Soft Delete (DELETE)
    public void deleteStaff(Long id) {
        User existingStaff = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Staff မတွေ့ရှိပါ။ ID: " + id));
        
        existingStaff.setStatus("INACTIVE"); // အပြီးမဖျက်ဘဲ Inactive သာ လုပ်လိုက်သည်
        existingStaff.setModifiedAt(LocalDateTime.now());
        
        userRepository.save(existingStaff);
    }
}
