package com.ojt.ecommerce.backofficeinventory.seeder;

import com.ojt.ecommerce.backofficeinventory.repository.PermissionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Permission;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet; // အသစ်ထည့်ရန်
import java.util.List;    // အသစ်ထည့်ရန်

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // ၁။ Permissions များကို ဖန်တီးပါမည်
        Permission manageStaff = createPermissionIfNotFound("MANAGE_STAFF");
        Permission manageProduct = createPermissionIfNotFound("MANAGE_PRODUCT");
        Permission viewOrder = createPermissionIfNotFound("VIEW_ORDERS");
        Permission manageInventory = createPermissionIfNotFound("MANAGE_INVENTORY");

        // ၂။ Role များဖန်တီးပြီး HashSet ဖြင့် Mutable (ပြင်ဆင်နိုင်သော) Set ကို သုံးမည်
        
        // (က) Admin Role
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> Role.builder().roleName("ADMIN").build());
        adminRole.setPermissions(new HashSet<>(List.of(manageStaff, manageProduct, viewOrder, manageInventory)));
        roleRepository.save(adminRole);

        // (ခ) Inventory Staff Role
        Role inventoryRole = roleRepository.findByRoleName("INVENTORY_STAFF")
                .orElseGet(() -> Role.builder().roleName("INVENTORY_STAFF").build());
        inventoryRole.setPermissions(new HashSet<>(List.of(manageProduct, manageInventory)));
        roleRepository.save(inventoryRole);

        // (ဂ) Sales Staff Role
        Role salesRole = roleRepository.findByRoleName("SALES_STAFF")
                .orElseGet(() -> Role.builder().roleName("SALES_STAFF").build());
        salesRole.setPermissions(new HashSet<>(List.of(viewOrder)));
        roleRepository.save(salesRole);

        // ၃။ Users များကို ဖန်တီးပါမည်
        if (userRepository.findByEmail("admin@ecommerce.com").isEmpty()) {
            User adminUser = User.builder()
                    .userName("System Admin")
                    .email("admin@ecommerce.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .userRole(adminRole)
                    .build();
            userRepository.save(adminUser);
        }

        if (userRepository.findByEmail("inventory@ecommerce.com").isEmpty()) {
            User inventoryUser = User.builder()
                    .userName("Inventory Manager")
                    .email("inventory@ecommerce.com")
                    .passwordHash(passwordEncoder.encode("inv123"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .userRole(inventoryRole)
                    .build();
            userRepository.save(inventoryUser);
        }

        if (userRepository.findByEmail("sales@ecommerce.com").isEmpty()) {
            User salesUser = User.builder()
                    .userName("Sales Representative")
                    .email("sales@ecommerce.com")
                    .passwordHash(passwordEncoder.encode("sales123"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .userRole(salesRole)
                    .build();
            userRepository.save(salesUser);
        }
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByPermissionName(name).orElseGet(() -> {
            return permissionRepository.save(Permission.builder().permissionName(name).build());
        });
    }
}