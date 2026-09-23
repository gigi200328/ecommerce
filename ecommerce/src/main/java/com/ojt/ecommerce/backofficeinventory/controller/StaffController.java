package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.StaffRequestDTO;
import com.ojt.ecommerce.backofficeinventory.dto.StaffResponseDTO;
import com.ojt.ecommerce.backofficeinventory.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // Staff အသစ်ဖန်တီးရန် API (ADMIN များသာ သုံးခွင့်ရှိရန် SecurityConfig တွင် ကန့်သတ်ထားပြီးဖြစ်သည်)
    @PostMapping
    public ResponseEntity<StaffResponseDTO> createStaff(@RequestBody StaffRequestDTO request) {
        StaffResponseDTO response = staffService.createStaff(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    // Staff တစ်ယောက်ချင်းစီ၏ အချက်အလက်ကို ပြင်ဆင်ရန်
    @PutMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> updateStaff(
            @PathVariable Long id, 
            @RequestBody StaffRequestDTO request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    // Staff အကောင့်ကို ပိတ်ရန် (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok("Staff အကောင့်ကို အောင်မြင်စွာ ပိတ်သိမ်း (Inactive) လိုက်ပါပြီ။");
    }
}