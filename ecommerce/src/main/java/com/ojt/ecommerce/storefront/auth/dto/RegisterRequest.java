package com.ojt.ecommerce.storefront.auth.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class RegisterRequest {
    @NotBlank private String fullName;
    @NotBlank @Email private String email;
    private String phone;
    @NotBlank private String password;
}