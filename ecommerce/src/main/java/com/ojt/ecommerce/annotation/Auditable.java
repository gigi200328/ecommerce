package com.ojt.ecommerce.annotation;

import com.ojt.ecommerce.entity.AuditAction;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String moduleName();               // ဥပမာ - "Staff Management"
    AuditAction action();            // ဥပမာ - AuditAction.UPDATE
    String tableName() default "";     // ဥပမာ - "users"
    String recordId() default "";      // SpEL Expression အတွက် ဥပမာ - "#id"
    String description() default "";
}