package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.StaffRequestDTO;
import com.ojt.ecommerce.backofficeinventory.dto.StaffResponseDTO;
import com.ojt.ecommerce.backofficeinventory.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Map;
@RestController
@RequestMapping("/api/backoffice/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    
    @PostMapping
    public ResponseEntity<StaffResponseDTO> createStaff(@RequestBody StaffRequestDTO request) {
        StaffResponseDTO response = staffService.createStaff(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @GetMapping
    public ResponseEntity<List<StaffResponseDTO>> getAllStaff() {
        return ResponseEntity.ok(staffService.getAllStaff());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponseDTO> updateStaff(
            @PathVariable Long id, 
            @RequestBody StaffRequestDTO request) {
        return ResponseEntity.ok(staffService.updateStaff(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStaff(@PathVariable Long id) {
        staffService.deleteStaff(id);
        return ResponseEntity.ok("Staff အကောင့်ကို အောင်မြင်စွာ ပိတ်သိမ်း (Inactive) လိုက်ပါပြီ။");
    }
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStaffStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            staffService.updateStaffStatus(id, status);
            return ResponseEntity.ok(Map.of("message", "Status အောင်မြင်စွာ ပြောင်းလဲပြီးပါပြီ။"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}