package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/backoffice/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Audit Log မှတ်တမ်းများကို ADMIN သာလျှင် ကြည့်ရှုနိုင်ခွင့်ရှိမည်။
     * Pagination Parameter (page, size) ပေးပို့ပါက Page အလိုက် ပြန်ပေးမည်ဖြစ်ပြီး၊
     * မပေးပို့ပါက Latest First စီတန်းထားသော List အဖြစ် ပြန်ပေးပါမည်။
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        if (page != null) {
            int pageSize = (size != null && size > 0) ? size : 20;
            Pageable pageable = PageRequest.of(page, pageSize);
            return ResponseEntity.ok(auditLogService.getAuditLogsPaged(pageable));
        }

        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }
}
