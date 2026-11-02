package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.repository.AuditLogRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.AuditAction;
import com.ojt.ecommerce.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List; // <--- List အတွက် Import အသစ်

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    public void logAction(AuditAction action, String moduleName, String tableName, 
                          Long recordId, String description, 
                          String oldValue, String newValue, String status) {
        
        Long userId = null;
        String userName = "SYSTEM / ANONYMOUS";

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            userId = userDetails.getUser().getUserId();
            userName = userDetails.getUser().getUserName();
        }

        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        String userAgent = request.getHeader("User-Agent");
        String endpoint = request.getRequestURI();
        String httpMethod = request.getMethod();

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .moduleName(moduleName)
                .tableName(tableName)
                .recordId(recordId)
                .description(description)
                .oldValue(oldValue)
                .newValue(newValue)
                .httpMethod(httpMethod)
                .endpoint(endpoint)
                .userId(userId)
                .userName(userName)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(status)
                .build();

        auditLogRepository.save(auditLog);
    }

    // --- Controller အတွက် အသစ်ထပ်ထည့်ထားသော Method ---
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
}