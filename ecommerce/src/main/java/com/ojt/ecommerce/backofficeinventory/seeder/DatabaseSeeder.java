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

        Role adminRole = roleRepository.findByRoleName("ADMIN").orElseGet(() -> {
            Role newRole = Role.builder().roleName("ADMIN").build();
            return roleRepository.save(newRole);
        });

        roleRepository.findByRoleName("STAFF").orElseGet(() -> {
            Role newRole = Role.builder().roleName("STAFF").build();
            return roleRepository.save(newRole);
        });
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
    }
}