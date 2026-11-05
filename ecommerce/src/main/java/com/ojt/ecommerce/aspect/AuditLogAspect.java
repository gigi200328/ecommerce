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
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @PostConstruct
    public void setupObjectMapper() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @Around("@annotation(auditable)")
    public Object logAuditActivity(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        Long recordId = extractEntityId(args, null);
        String oldValue = null;
        String newValue = null;

        // ၁။ UPDATE သို့မဟုတ် DELETE မတိုင်မီ Database ထဲရှိ မူလ Data (Old Value) ကို ကြိုတင် Snapshot ရယူခြင်း
        if ((auditable.action() == AuditAction.UPDATE || auditable.action() == AuditAction.DELETE) && recordId != null) {
            try {
                Class<?> entityClass = findEntityClass(auditable.moduleName(), auditable.tableName());
                if (entityClass != null) {
                    Object existingEntity = entityManager.find(entityClass, recordId);
                    if (existingEntity != null) {
                        Map<String, Object> snapshot = extractEntitySnapshot(existingEntity);
                        oldValue = serializePayloadSafely(snapshot);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not capture old value before {}: {}", methodName, e.getMessage());
            }
        }

        // Request Body / Payload ကို ရှာဖွေဖမ်းယူခြင်း
        Object payload = findPayload(args);
        if (auditable.action() == AuditAction.CREATE || auditable.action() == AuditAction.UPDATE) {
            if (payload != null) {
                newValue = serializePayloadSafely(payload);
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
                // CREATE သို့မဟုတ် args ထဲတွင် ID မပါခဲ့ပါက Result ထံမှ recordId ရယူခြင်း
                if (recordId == null) {
                    recordId = extractEntityId(args, result);
                }

                // New Value ကို Arguments ထဲတွင် ရှာမတွေ့ပါက (ဥပမာ QueryParam ဖြင့် update လုပ်သည့်အခါ) Response Body ထံမှ ရယူခြင်း
                if (newValue == null && result != null && 
                    (auditable.action() == AuditAction.CREATE || auditable.action() == AuditAction.UPDATE)) {
                    Object body = (result instanceof ResponseEntity<?> re) ? re.getBody() : result;
                    if (body != null) {
                        newValue = serializePayloadSafely(body);
                    }
                }

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
     * Module Name / Table Name မှ သက်ဆိုင်ရာ JPA Entity Class ကို ရှာဖွေခြင်း
     */
    private Class<?> findEntityClass(String moduleName, String tableName) {
        String hint = (tableName != null && !tableName.isBlank()) ? tableName : moduleName;
        if (hint == null) return null;
        String clean = hint.toLowerCase().replaceAll("[^a-z0-9]", "");

        try {
            for (EntityType<?> entityType : entityManager.getMetamodel().getEntities()) {
                String entityName = entityType.getName().toLowerCase();
                Class<?> javaType = entityType.getJavaType();
                if (javaType == null) continue;

                if (clean.contains(entityName) || entityName.contains(clean)) {
                    return javaType;
                }

                Table table = javaType.getAnnotation(Table.class);
                if (table != null && !table.name().isBlank()) {
                    String tbl = table.name().toLowerCase().replaceAll("[^a-z0-9]", "");
                    if (clean.contains(tbl) || tbl.contains(clean)) {
                        return javaType;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error finding entity class for {}: {}", hint, e.getMessage());
        }

        // Direct fallback checks
        if (clean.contains("staff") || clean.contains("user")) {
            try { return Class.forName("com.ojt.ecommerce.entity.User"); } catch (Exception ignored) {}
        }
        if (clean.contains("category")) {
            try { return Class.forName("com.ojt.ecommerce.entity.Category"); } catch (Exception ignored) {}
        }
        if (clean.contains("product")) {
            try { return Class.forName("com.ojt.ecommerce.entity.Product"); } catch (Exception ignored) {}
        }
        if (clean.contains("brand")) {
            try { return Class.forName("com.ojt.ecommerce.entity.Brand"); } catch (Exception ignored) {}
        }
        if (clean.contains("tag")) {
            try { return Class.forName("com.ojt.ecommerce.entity.Tag"); } catch (Exception ignored) {}
        }
        if (clean.contains("order")) {
            try { return Class.forName("com.ojt.ecommerce.entity.Order"); } catch (Exception ignored) {}
        }

        return null;
    }

    /**
     * Entity တစ်ခု၏ Data များကို Infinite Loop / Lazy Loading မဖြစ်စေဘဲ သန့်ရှင်းစွာ Snapshot ထုတ်ယူခြင်း
     */
    private Map<String, Object> extractEntitySnapshot(Object entity) {
        if (entity == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();

        try {
            BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
            PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();

            for (PropertyDescriptor pd : descriptors) {
                String name = pd.getName();
                if ("class".equals(name) || "hibernateLazyInitializer".equals(name) || "handler".equals(name)) {
                    continue;
                }
                Class<?> propType = pd.getPropertyType();
                if (propType == null) continue;

                // Skip Collections / Maps to avoid infinite recursion and uninitialized lazy collections
                if (Collection.class.isAssignableFrom(propType) || Map.class.isAssignableFrom(propType)) {
                    continue;
                }

                Object value = wrapper.getPropertyValue(name);
                if (value == null) {
                    map.put(name, null);
                } else if (isSimpleType(propType)) {
                    map.put(name, value);
                } else {
                    // For related Entities (e.g. userRole, parentCategory): extract readable identifier
                    try {
                        BeanWrapper relWrapper = PropertyAccessorFactory.forBeanPropertyAccess(value);
                        if (relWrapper.isReadableProperty("roleName")) {
                            map.put(name, relWrapper.getPropertyValue("roleName"));
                        } else if (relWrapper.isReadableProperty("categoryName")) {
                            map.put(name, relWrapper.getPropertyValue("categoryName"));
                        } else if (relWrapper.isReadableProperty("name")) {
                            map.put(name, relWrapper.getPropertyValue("name"));
                        } else if (relWrapper.isReadableProperty("id")) {
                            map.put(name + "Id", relWrapper.getPropertyValue("id"));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract entity snapshot: {}", e.getMessage());
        }
        return map;
    }

    private boolean isSimpleType(Class<?> type) {
        return type.isPrimitive() ||
               type.equals(String.class) ||
               type.equals(Boolean.class) ||
               type.equals(Byte.class) ||
               type.equals(Short.class) ||
               type.equals(Integer.class) ||
               type.equals(Long.class) ||
               type.equals(Float.class) ||
               type.equals(Double.class) ||
               type.equals(java.math.BigDecimal.class) ||
               type.equals(java.math.BigInteger.class) ||
               type.equals(java.time.LocalDate.class) ||
               type.equals(java.time.LocalDateTime.class) ||
               type.equals(java.time.LocalTime.class) ||
               type.equals(java.util.Date.class) ||
               type.isEnum();
    }

    /**
     * Arguments ထဲမှ Serialized လုပ်နိုင်သော Payload DTO ကို ရွေးထုတ်ခြင်း
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
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof Long l) {
                    return l;
                } else if (arg instanceof Integer i) {
                    return i.longValue();
                }
            }
        }

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
