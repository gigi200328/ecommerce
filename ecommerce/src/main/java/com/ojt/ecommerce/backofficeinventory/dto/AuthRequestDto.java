package com.ojt.ecommerce.backofficeinventory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data 
public class AuthRequestDto {
    
    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format. (e.g., admin@ecommerce.com)")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters long.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z]).+$", 
             message = "Password must contain at least one uppercase letter (A-Z) and one lowercase letter (a-z).")
    private String password;
}