package com.ojt.ecommerce.backofficeinventory.seeder;

import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // ၁။ Admin Role
        Role adminRole = roleRepository.findByRoleName("ADMIN").orElseGet(() -> {
            Role newRole = Role.builder().roleName("ADMIN").build();
            return roleRepository.save(newRole);
        });

        // ၂။ Inventory Staff Role
        Role inventoryRole = roleRepository.findByRoleName("INVENTORY_STAFF").orElseGet(() -> {
            Role newRole = Role.builder().roleName("INVENTORY_STAFF").build();
            return roleRepository.save(newRole);
        });

        // ၃။ Sales Staff Role
        Role salesRole = roleRepository.findByRoleName("SALES_STAFF").orElseGet(() -> {
            Role newRole = Role.builder().roleName("SALES_STAFF").build();
            return roleRepository.save(newRole);
        });

        // Admin User
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
            System.out.println("✅ Default Admin User created successfully!");
        }

        // Inventory Staff User
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
            System.out.println("✅ Default Inventory Staff created successfully!");
        }

        // Sales Staff User
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
            System.out.println("✅ Default Sales Staff created successfully!");
        }
    }
}