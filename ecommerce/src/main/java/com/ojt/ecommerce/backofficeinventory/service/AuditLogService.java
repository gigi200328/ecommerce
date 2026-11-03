package com.ojt.ecommerce.backofficeinventory.service;

import com.ojt.ecommerce.backofficeinventory.repository.AuditLogRepository;
import com.ojt.ecommerce.backofficeinventory.security.CustomUserDetails;
import com.ojt.ecommerce.entity.AuditAction;
import com.ojt.ecommerce.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * အဓိက Log မှတ်တမ်းတင်သည့် Method
     */
    public void logAction(AuditAction action, String moduleName, String tableName, 
                          Long recordId, String description, 
                          String oldValue, String newValue, String status) {
        logAction(action, moduleName, tableName, recordId, description, oldValue, newValue, status, null, null);
    }

    /**
     * User ID / User Name တိုက်ရိုက်သတ်မှတ်နိုင်သော Overloaded Method (Login စသည့် အခြေအနေများအတွက်)
     */
    public void logAction(AuditAction action, String moduleName, String tableName, 
                          Long recordId, String description, 
                          String oldValue, String newValue, String status,
                          Long explicitUserId, String explicitUserName) {
        try {
            Long userId = explicitUserId;
            String userName = explicitUserName;

            // Explicit user မပါရှိပါက SecurityContext မှ အလိုအလျောက် ယူမည်
            if (userName == null || userName.isBlank()) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated() && 
                    !"anonymousUser".equals(authentication.getPrincipal())) {
                    if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
                        if (userId == null && userDetails.getUser() != null) {
                            userId = userDetails.getUser().getUserId();
                        }
                        userName = userDetails.getUsername();
                    } else {
                        userName = authentication.getName();
                    }
                } else {
                    userName = "SYSTEM / ANONYMOUS";
                }
            }

            // HTTP Request မှ Request Details များကို ရယူခြင်း
            String ipAddress = null;
            String userAgent = null;
            String endpoint = null;
            String httpMethod = null;

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isBlank() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                } else {
                    ipAddress = ipAddress.split(",")[0].trim();
                }
                userAgent = request.getHeader("User-Agent");
                endpoint = request.getRequestURI();
                httpMethod = request.getMethod();
            }

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
                    .status(status != null ? status : "SUCCESS")
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Login / Logout စသည့် Authentication ဆိုင်ရာ Log များအတွက် Helper Method
     */
    public void logAuthAction(AuditAction action, String description, Long userId, String userName, String status) {
        logAction(action, "Authentication", "users", userId, description, null, null, status, userId, userName);
    }

    // --- Controller များအတွက် Log များ ရယူရန် Methods ---
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<AuditLog> getAuditLogsPaged(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * ရက်စွဲအပိုင်းအခြား (Date Range) ဖြင့် စစ်ထုတ်၍ Audit Log များ ရယူခြင်း
     */
    public List<AuditLog> getAuditLogs(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        if (startDateTime != null || endDateTime != null) {
            return auditLogRepository.findByDateRange(startDateTime, endDateTime);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * ရက်စွဲအပိုင်းအခြား (Date Range) ဖြင့် စစ်ထုတ်၍ Pagination ဖြင့် ရယူခြင်း
     */
    public Page<AuditLog> getAuditLogsPaged(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        if (startDateTime != null || endDateTime != null) {
            return auditLogRepository.findByDateRange(startDateTime, endDateTime, pageable);
        }
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
