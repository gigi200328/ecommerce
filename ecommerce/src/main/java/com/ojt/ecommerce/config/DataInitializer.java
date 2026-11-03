package com.ojt.ecommerce.config;

import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initializeRolesAndAdmin();
    }

    private void initializeRolesAndAdmin() {
        // ၁။ အခြေခံ Role များ (ADMIN, INVENTORY_STAFF, SALES_STAFF) ရှိမရှိ စစ်ဆေးပြီး မရှိပါက ဖန်တီးပေးခြင်း
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> {
                    log.info("Role 'ADMIN' မရှိသေးသဖြင့် အသစ်ဖန်တီးနေပါသည်...");
                    return roleRepository.save(Role.builder().roleName("ADMIN").build());
                });

        roleRepository.findByRoleName("INVENTORY_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("INVENTORY_STAFF").build()));

        roleRepository.findByRoleName("SALES_STAFF")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("SALES_STAFF").build()));

        // ၂။ စနစ်ထဲတွင် ACTIVE ဖြစ်နေသော ADMIN အကောင့် ရှိမရှိ စစ်ဆေးခြင်း
        long activeAdminCount = userRepository.countByRoleNameAndStatus("ADMIN", "ACTIVE");

        if (activeAdminCount == 0) {
            log.warn("စနစ်တွင် Active ဖြစ်နေသော ADMIN အကောင့် မတွေ့ရှိပါ။ Default Super Admin အား အလိုအလျောက် ဖန်တီးနေပါသည်...");

            // admin@ecommerce.com ဖြင့် User အဟောင်း ရှိမရှိ စစ်ဆေးခြင်း
            User defaultAdmin = userRepository.findByEmail("admin@ecommerce.com")
                    .orElse(
                            User.builder()
                                    .userName("Super Admin")
                                    .email("admin@ecommerce.com")
                                    .createdAt(LocalDateTime.now())
                                    .build()
                    );

            defaultAdmin.setUserRole(adminRole);
            // Frontend ၏ Password Validation (အနည်းဆုံး ၈ လုံး၊ အကြီး A-Z၊ အသေး a-z ပါဝင်ရမည်) နှင့် ကိုက်ညီစေရန်
            defaultAdmin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            defaultAdmin.setStatus("ACTIVE");
            defaultAdmin.setTokenVersion(1);
            defaultAdmin.setModifiedAt(LocalDateTime.now());

            userRepository.save(defaultAdmin);

            log.info("==================================================================");
            log.info("✅ Default Super Admin အောင်မြင်စွာ တည်ဆောက်ပြီးပါပြီ။");
            log.info("👉 Login Email    : admin@ecommerce.com");
            log.info("👉 Login Password : Admin@123");
            log.info("👉 Role           : ADMIN");
            log.info("==================================================================");
        } else {
            log.info("Active ADMIN အကောင့် ({} ခု) ရှိနေပြီး ဖြစ်သောကြောင့် Seeder ကျော်သွားပါမည်။", activeAdminCount);
        }
    }
}
