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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Enumeration;
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
                ipAddress = resolveClientIp(request);
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
     * Client ၏ တကယ့် IP Address (WiFi / LAN / Proxy IP) ကို စစ်ထုတ်ရယူခြင်း
     */
    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        String ip = null;
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                ip = value.split(",")[0].trim();
                break;
            }
        }

        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Localhost / Loopback ဖြစ်နေပါက အသုံးပြုနေသော လက်ရှိ WiFi / LAN IP address ကို ရယူမည်
        if (ip != null && (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1") || ip.equals("127.0.0.1"))) {
            String lanIp = getActiveLanIp();
            if (lanIp != null && !lanIp.isBlank()) {
                ip = lanIp;
            }
        }

        return ip != null ? ip : "127.0.0.1";
    }

    /**
     * လက်ရှိစက်၏ ချိတ်ဆက်ထားသော WiFi / LAN IPv4 Address ကို ရယူခြင်း
     */
    private String getActiveLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost != null && !localHost.isLoopbackAddress()) {
                return localHost.getHostAddress();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
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
