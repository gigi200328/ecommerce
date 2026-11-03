package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.StaffRequestDTO;
import com.ojt.ecommerce.backofficeinventory.dto.StaffResponseDTO;
import com.ojt.ecommerce.backofficeinventory.service.StaffService;
import com.ojt.ecommerce.annotation.Auditable; 
// သင့် Project ရှိ AuditAction (Enum) ရှိရာ လမ်းကြောင်းကို Import လုပ်ပါ
import com.ojt.ecommerce.entity.AuditAction; 

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backoffice/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // Staff အသစ်ဖန်တီးရန် (POST) - မှတ်တမ်းတင်မည်
    @Auditable(action = AuditAction.CREATE, moduleName = "Staff Management")
    @PostMapping
    public ResponseEntity<StaffResponseDTO> createStaff(@RequestBody StaffRequestDTO request) {
        return ResponseEntity.ok(staffService.createStaff(request));
    }

    // Staff အားလုံးကို ယူရန် (GET) - ဤအရာကို မှတ်တမ်းတင်လေ့မရှိပါ
    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    // Edit Form အတွက် GET API - ဤအရာကို မှတ်တမ်းတင်လေ့မရှိပါ
    @GetMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> getStaffById(@PathVariable Long id) {
        return ResponseEntity.ok(staffService.getStaffById(id));
    }

    // Staff အချက်အလက် ပြင်ဆင်ရန် (PUT) - မှတ်တမ်းတင်မည်
    @Auditable(action = AuditAction.UPDATE, moduleName = "Staff Management")
    @PutMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> updateStaff(@PathVariable Long id, @RequestBody StaffRequestDTO request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    // Staff ၏ Status (Active/Inactive) ပြောင်းလဲရန် (PUT) - မှတ်တမ်းတင်မည်
    @Auditable(action = AuditAction.UPDATE, moduleName = "Staff Management")
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStaffStatus(@PathVariable Long id, @RequestParam String status) {
        staffService.updateStaffStatus(id, status);
        return ResponseEntity.ok().build();
    }

    // Staff အား ဖျက်ရန် (DELETE) - မှတ်တမ်းတင်မည်
    @Auditable(action = AuditAction.DELETE, moduleName = "Staff Management")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok().build();
    }
}