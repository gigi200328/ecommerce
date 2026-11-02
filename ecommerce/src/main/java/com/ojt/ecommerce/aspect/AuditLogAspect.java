package com.ojt.ecommerce.aspect; // သင့် Project ၏ package အတိုင်း ပြင်ပါ

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ojt.ecommerce.annotation.Auditable;
import com.ojt.ecommerce.entity.AuditAction;
import com.ojt.ecommerce.backofficeinventory.service.AuditLogService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // LocalDateTime များကို JSON ပြောင်းရာတွင် Error မတက်စေရန် Configuration
    @PostConstruct
    public void setupObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Around("@annotation(auditable)")
    public Object logAuditActivity(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        String oldValue = null;
        String newValue = null;
        Long entityId = null;

        // Old Value နှင့် New Value များကို ဖမ်းယူခြင်း
        try {
            if (args.length > 0) {
                // ပထမဆုံး Parameter သည် ID ဖြစ်တတ်သည် (ဥပမာ - update(Long id, Dto dto))
                if (args[0] instanceof Long) {
                    entityId = (Long) args[0];
                }
                
                // CREATE သို့မဟုတ် UPDATE ဖြစ်လျှင် Request Body အား newValue အဖြစ် ဖမ်းမည်
                if (auditable.action() == AuditAction.CREATE || auditable.action() == AuditAction.UPDATE) {
                    Object payload = args.length > 1 ? args[1] : args[0];
                    newValue = objectMapper.writeValueAsString(payload);
                } 
                // DELETE ဖြစ်လျှင် ဖျက်မည့် Data ကို oldValue အဖြစ် ဖမ်းမည်
                else if (auditable.action() == AuditAction.DELETE) {
                    oldValue = objectMapper.writeValueAsString(args[0]);
                }
            }
        } catch (Exception e) {
            System.err.println("JSON Serialization Error: " + e.getMessage());
        }

        // မူလ Controller Method ကို အလုပ်လုပ်စေခြင်း
        Object result = joinPoint.proceed();

        // ပြီးဆုံးပါက Audit Log သိမ်းဆည်းခြင်း
        try {
            String username = "SYSTEM";
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
            }

            String ipAddress = null;
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getRemoteAddr();
            }

            auditLogService.logAction(
                    auditable.action(),
                    auditable.moduleName(),
                    "Executed method: " + methodName,
                    entityId,
                    oldValue,
                    newValue,
                    username,
                    ipAddress
            );
        } catch (Exception e) {
            System.err.println("Failed to save audit log: " + e.getMessage());
        }

        return result;
    }
}