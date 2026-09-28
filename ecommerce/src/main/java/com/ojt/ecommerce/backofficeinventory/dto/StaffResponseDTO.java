package com.ojt.ecommerce.backofficeinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponseDTO {
    
    private Long userId;
    private String userName;
    private String email;
    private String roleName;
    private String status;
    private LocalDateTime createdAt;
    
}