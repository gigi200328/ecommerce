package com.ojt.ecommerce.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ojt.ecommerce.annotation.Auditable;
import com.ojt.ecommerce.backofficeinventory.service.AuditLogService;
import com.ojt.ecommerce.entity.AuditAction;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void setupObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Around("@annotation(auditable)")
    public Object logAuditActivity(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        // Request Body / Payload ကို ရှာဖွေဖမ်းယူခြင်း
        Object payload = findPayload(args);
        String newValue = null;
        String oldValue = null;

        if (auditable.action() == AuditAction.CREATE || auditable.action() == AuditAction.UPDATE) {
            newValue = serializePayloadSafely(payload);
        } else if (auditable.action() == AuditAction.DELETE) {
            if (payload != null) {
                oldValue = serializePayloadSafely(payload);
            }
        }

        Object result = null;
        Throwable caughtException = null;

        try {
            // Controller method ကို execute လုပ်ခြင်း
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            caughtException = t;
            throw t;
        } finally {
            try {
                // Entity ID ကို arguments သို့မဟုတ် response result ထံမှ ရှာဖွေရယူခြင်း
                Long recordId = extractEntityId(args, result);
                String status = (caughtException == null) ? "SUCCESS" : "FAILED";

                String description = auditable.description();
                if (description == null || description.isBlank()) {
                    description = "Executed method: " + methodName;
                }
                if (caughtException != null) {
                    description += " [Error: " + caughtException.getMessage() + "]";
                }

                String tableName = auditable.tableName();
                if (tableName == null || tableName.isBlank()) {
                    tableName = auditable.moduleName();
                }

                // AuditLogService သို့ parameter အစဉ်လိုက် မှန်ကန်စွာ ပေးပို့ခြင်း
                auditLogService.logAction(
                        auditable.action(),
                        auditable.moduleName(),
                        tableName,
                        recordId,
                        description,
                        oldValue,
                        newValue,
                        status
                );
            } catch (Exception e) {
                log.error("Failed to record audit log for method {}: {}", methodName, e.getMessage(), e);
            }
        }
    }

    /**
     * Arguments ထဲမှ Serialized လုပ်နိုင်သော Payload DTO ကို ရွေးထုတ်ခြင်း
     * (MultipartFile, Request, Principal စသည်တို့ကို ကျော်ပါမည်)
     */
    private Object findPayload(Object[] args) {
        if (args == null || args.length == 0) return null;

        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof MultipartFile || arg instanceof MultipartFile[] ||
                arg instanceof HttpServletRequest || arg instanceof HttpServletResponse ||
                arg instanceof BindingResult || arg instanceof Principal) {
                continue;
            }
            if (arg instanceof Long || arg instanceof Integer) {
                // PathVariable ID များ ဖြစ်တတ်သောကြောင့် အခြား object များ ရှိမရှိ ဆက်ရှာမည်
                continue;
            }
            return arg;
        }
        return null;
    }

    /**
     * Payload ကို JSON ပြောင်းရာတွင် Password / Secret များကို Mask ပြုလုပ်ပေးခြင်း
     */
    private String serializePayloadSafely(Object payload) {
        if (payload == null) return null;
        try {
            JsonNode node = objectMapper.valueToTree(payload);
            maskSensitiveData(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Failed to serialize audit payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Password, Token စသည့် sensitive field များကို "******" ဖြင့် အစားထိုးခြင်း
     */
    private void maskSensitiveData(JsonNode node) {
        if (node == null) return;

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> toMask = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey().toLowerCase();
                if (fieldName.contains("password") || fieldName.contains("secret") || fieldName.contains("token")) {
                    toMask.add(field.getKey());
                } else if (field.getValue().isContainerNode()) {
                    maskSensitiveData(field.getValue());
                }
            }
            for (String key : toMask) {
                objectNode.put(key, "******");
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                maskSensitiveData(child);
            }
        }
    }

    /**
     * Request Args သို့မဟုတ် Response Result ထဲမှ Record ID ကို ထုတ်ယူခြင်း
     */
    private Long extractEntityId(Object[] args, Object result) {
        // ၁။ Argument များထဲတွင် Long ID ပါမပါ စစ်ဆေးခြင်း
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof Long l) {
                    return l;
                } else if (arg instanceof Integer i) {
                    return i.longValue();
                }
            }
        }

        // ၂။ CREATE ဖြစ်ပါက Result (ResponseEntity) ထဲမှ ID ကို ဆွဲထုတ်ခြင်း
        if (result != null) {
            Object target = result;
            if (result instanceof ResponseEntity<?> responseEntity) {
                target = responseEntity.getBody();
            }
            if (target != null) {
                try {
                    Method[] methods = target.getClass().getMethods();
                    for (Method m : methods) {
                        if (m.getName().matches("getId|get.*Id") && m.getParameterCount() == 0 &&
                            (m.getReturnType() == Long.class || m.getReturnType() == long.class)) {
                            Object val = m.invoke(target);
                            if (val instanceof Long l) return l;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }
}
