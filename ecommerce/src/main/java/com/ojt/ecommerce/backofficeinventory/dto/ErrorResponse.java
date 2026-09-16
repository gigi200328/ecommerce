package com.ojt.ecommerce.backofficeinventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    
    // Request Validation (Validation Errors) ဖြစ်ပါက Field အလိုက် ပြပေးရန်
    private Map<String, String> validationErrors;
}
