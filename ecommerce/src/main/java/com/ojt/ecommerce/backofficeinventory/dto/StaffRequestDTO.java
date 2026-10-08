package com.ojt.ecommerce.backofficeinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRequestDTO {
   
    private String userName;
    private String email;
    private String password;
    private String roleName; // ဥပမာ - "INVENTORY_STAFF" သို့မဟုတ် "SALES_STAFF"
    
}