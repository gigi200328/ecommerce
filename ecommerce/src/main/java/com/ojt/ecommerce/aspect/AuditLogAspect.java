package com.ojt.ecommerce.aspect;

import com.ojt.ecommerce.annotation.Auditable;
import com.ojt.ecommerce.backofficeinventory.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    @Around("@annotation(auditable)")
    public Object logAuditActivity(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = null;
        String status = "SUCCESS";

        try {
            // မူလ API Method ကို အလုပ်လုပ်စေခြင်း
            result = joinPoint.proceed(); 
            return result;
        } catch (Throwable ex) {
            status = "FAILED";
            throw ex;
        } finally {
            // Parameter ထဲမှ #id ကို SpEL ဖြင့် ဆွဲထုတ်ခြင်း
            Long recordId = resolveRecordId(auditable, joinPoint, result);

            // AuditLogService သို့ ပေးပို့၍ Database တွင် သိမ်းဆည်းခြင်း
            auditLogService.logAction(
                    auditable.action(),
                    auditable.moduleName(),
                    auditable.tableName(),
                    recordId,
                    auditable.description(),
                    null, // AOP ဖြင့် JSON အဟောင်း/အသစ် ယူရန်ခက်ခဲသဖြင့် လွတ်ထားနိုင်သည်
                    null, 
                    status
            );
        }
    }

    // @Auditable(recordId = "#id") ဟု ရေးထားပါက Method Parameter ရှိ id တန်ဖိုးကို အလိုအလျောက် ရှာဖွေပေးမည့် Method
    private Long resolveRecordId(Auditable auditable, ProceedingJoinPoint joinPoint, Object result) {
        String expression = auditable.recordId();
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] names = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            context.setVariable("result", result);

            Object value = SPEL_PARSER.parseExpression(expression).getValue(context);
            return value != null ? Long.valueOf(value.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }
}