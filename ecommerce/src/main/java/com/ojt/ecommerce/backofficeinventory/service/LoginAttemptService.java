package com.ojt.ecommerce.backofficeinventory.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-Force Password Protection Service
 * အကောင့် Email နှင့် IP အလိုက် Password မှားယွင်းမှု အကြိမ်ရေကို စောင့်ကြည့်ပြီး
 * ၅ ကြိမ် ဆက်တိုက် မှားယွင်းပါက ၁၅ မိနစ် ယာယီ Lock ချပေးသည့် စနစ်
 * Super Admin (admin@ecommerce.com) သည် အခြားသူများ၏ အမှားများကြောင့် Lock မကျစေရန် ကာကွယ်ထားပါသည်
 */
@Service
@Slf4j
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_TIME_MINUTES = 15;

    @Getter
    public static class AttemptRecord {
        private int failedAttempts = 0;
        private LocalDateTime lockExpiresAt = null;
        private LocalDateTime lastAttemptAt = LocalDateTime.now();

        public void increment() {
            this.failedAttempts++;
            this.lastAttemptAt = LocalDateTime.now();
            if (this.failedAttempts >= MAX_ATTEMPTS) {
                this.lockExpiresAt = LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES);
            }
        }

        public boolean isCurrentlyLocked() {
            if (lockExpiresAt == null) return false;
            if (LocalDateTime.now().isBefore(lockExpiresAt)) {
                return true;
            }
            // Lock ကာလ ကုန်ဆုံးသွားပါက အလိုအလျောက် Reset ပြုလုပ်မည်
            this.failedAttempts = 0;
            this.lockExpiresAt = null;
            return false;
        }

        public long getRemainingMinutes() {
            if (lockExpiresAt == null) return 0;
            Duration duration = Duration.between(LocalDateTime.now(), lockExpiresAt);
            return Math.max(1, duration.toMinutes() + 1); // e.g. 15, 14, 13...
        }
    }

    private final Map<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    private String getCacheKey(String email, String ipAddress) {
        if (email != null && !email.isBlank()) {
            return email.trim().toLowerCase() + ":" + (ipAddress != null ? ipAddress : "127.0.0.1");
        }
        return ipAddress != null ? ipAddress : "127.0.0.1";
    }

    /**
     * Account / IP Address အား ယာယီ Lock ချထားခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
     * Super Admin (admin@ecommerce.com) သည် Brute-force Lock ကြောင့် ဘယ်သောအခါမှ Login ပိတ်မခံရစေပါ
     */
    public boolean isBlocked(String email, String ipAddress) {
        if ("admin@ecommerce.com".equalsIgnoreCase(email)) {
            return false;
        }
        String key = getCacheKey(email, ipAddress);
        AttemptRecord record = attemptsCache.get(key);
        return record != null && record.isCurrentlyLocked();
    }

    public boolean isBlocked(String ipAddress) {
        return isBlocked(null, ipAddress);
    }

    /**
     * Lock ပြည့်ရန် ကျန်ရှိသော မိနစ်အရေအတွက်
     */
    public long getRemainingLockMinutes(String email, String ipAddress) {
        String key = getCacheKey(email, ipAddress);
        AttemptRecord record = attemptsCache.get(key);
        return record != null ? record.getRemainingMinutes() : 0;
    }

    public long getRemainingLockMinutes(String ipAddress) {
        return getRemainingLockMinutes(null, ipAddress);
    }

    /**
     * Login မအောင်မြင်သည့်အခါ အကြိမ်ရေ ၁ တိုးပြီး ကျန်ရှိသည့် အကြိမ်ရေကို ပြန်ပေးခြင်း
     */
    public int loginFailed(String email, String ipAddress) {
        if ("admin@ecommerce.com".equalsIgnoreCase(email)) {
            return MAX_ATTEMPTS;
        }
        String key = getCacheKey(email, ipAddress);
        AttemptRecord record = attemptsCache.computeIfAbsent(key, k -> new AttemptRecord());
        record.increment();
        int remaining = MAX_ATTEMPTS - record.getFailedAttempts();
        log.warn("Failed login attempt for {}: {}. Total failed attempts: {} / {}", 
                email != null ? email : "IP", ipAddress, record.getFailedAttempts(), MAX_ATTEMPTS);
        return Math.max(0, remaining);
    }

    public int loginFailed(String ipAddress) {
        return loginFailed(null, ipAddress);
    }

    /**
     * Login အောင်မြင်သည့်အခါ ထို Account / IP ၏ မှားယွင်းထားသော အကြိမ်ရေကို Reset ပြန်လုပ်ခြင်း
     */
    public void loginSucceeded(String email, String ipAddress) {
        if (email != null && !email.isBlank()) {
            attemptsCache.remove(getCacheKey(email, ipAddress));
        }
        if (ipAddress != null) {
            attemptsCache.remove(ipAddress);
        }
        log.info("Login succeeded for {}: {}. Reset failed attempts counter.", email, ipAddress);
    }

    public void loginSucceeded(String ipAddress) {
        loginSucceeded(null, ipAddress);
    }

    /**
     * Cache အားလုံးကို အရေးပေါ် Reset ပြုလုပ်ရန် Method
     */
    public void resetAll() {
        attemptsCache.clear();
        log.info("Reset all login lockout attempts cache.");
    }

    /**
     * Client ၏ IP Address ကို Request မှ ရှာဖွေရယူခြင်း
     */
    public String getClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";

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

        // Localhost / Loopback ဖြစ်နေပါက လက်ရှိစက်၏ ချိတ်ဆက်ထားသော WiFi / LAN IP ကို ရယူမည်
        if (ip != null && (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1") || ip.equals("127.0.0.1"))) {
            String lanIp = getActiveLanIp();
            if (lanIp != null && !lanIp.isBlank()) {
                ip = lanIp;
            }
        }

        return ip != null ? ip : "127.0.0.1";
    }

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
}
