package com.ojt.ecommerce.backofficeinventory.seeder;

import com.ojt.ecommerce.backofficeinventory.repository.PermissionRepository;
import com.ojt.ecommerce.backofficeinventory.repository.RoleRepository;
import com.ojt.ecommerce.backofficeinventory.repository.UserRepository;
import com.ojt.ecommerce.backofficeinventory.repository.CategoryRepository;
import com.ojt.ecommerce.backofficeinventory.repository.BrandRepository;
import com.ojt.ecommerce.backofficeinventory.repository.InventoryRepository;
import com.ojt.ecommerce.entity.Category;
import com.ojt.ecommerce.entity.Brand;
import com.ojt.ecommerce.enums.BrandStatus;
import com.ojt.ecommerce.entity.Permission;
import com.ojt.ecommerce.entity.Role;
import com.ojt.ecommerce.entity.User;
import com.ojt.ecommerce.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final InventoryRepository inventoryRepository;

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

        // (ဂ) Sales Staff & Order Staff Role
        Role salesRole = roleRepository.findByRoleName("SALES_STAFF")
                .orElseGet(() -> Role.builder().roleName("SALES_STAFF").build());
        salesRole.setPermissions(new HashSet<>(List.of(viewOrder)));
        roleRepository.save(salesRole);

        Role orderRole = roleRepository.findByRoleName("ORDER_STAFF")
                .orElseGet(() -> Role.builder().roleName("ORDER_STAFF").build());
        orderRole.setPermissions(new HashSet<>(List.of(viewOrder)));
        roleRepository.save(orderRole);

        // ၃။ Users များကို ဖန်တီးပါမည်
        if (userRepository.findByEmail("admin@ecommerce.com").isEmpty()) {
            User adminUser = User.builder()
                    .userName("System Admin")
                    .email("admin@ecommerce.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
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
                    .passwordHash(passwordEncoder.encode("Admin@123"))
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
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .userRole(salesRole)
                    .build();
            userRepository.save(salesUser);
        }

        if (userRepository.findByEmail("order@ecommerce.com").isEmpty()) {
            User orderUser = User.builder()
                    .userName("Order Representative")
                    .email("order@ecommerce.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .status("ACTIVE")
                    .createdAt(LocalDateTime.now())
                    .userRole(orderRole)
                    .build();
            userRepository.save(orderUser);
        }

        // ၄။ Default Categories & Brands (createdBy, modifiedBy, createdAt နှင့် modifiedAt အပြည့်အစုံဖြင့်)
        User defaultAdmin = userRepository.findByEmail("admin@ecommerce.com").orElse(null);
        if (defaultAdmin != null) {
            if (categoryRepository.count() == 0) {
                categoryRepository.saveAll(List.of(
                    Category.builder().categoryName("Clothing").description("Fashion apparel, garments, and casual wear").createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Category.builder().categoryName("Footwear").description("Shoes, sneakers, and sandals").createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Category.builder().categoryName("Cosmetics").description("Beauty and personal care products").createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Category.builder().categoryName("Beverages").description("Refreshing drinks and beverages").createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Category.builder().categoryName("Electronics").description("Phones, computers, and electronic gadgets").createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build()
                ));
            }
            if (brandRepository.count() == 0) {
                brandRepository.saveAll(List.of(
                    Brand.builder().brandName("Nike").description("Premium athletic footwear and sportswear").status(BrandStatus.ACTIVE).createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Brand.builder().brandName("Zara").description("Contemporary fashion and lifestyle").status(BrandStatus.ACTIVE).createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Brand.builder().brandName("Apple").description("Smart devices and consumer electronics").status(BrandStatus.ACTIVE).createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build(),
                    Brand.builder().brandName("Samsung").description("Electronics and modern hardware").status(BrandStatus.ACTIVE).createdBy(defaultAdmin).modifiedBy(defaultAdmin).createdAt(LocalDateTime.now()).modifiedAt(LocalDateTime.now()).build()
                ));
            }
        }

        // ၅။ Auto-backfill perishable inventory (e.g. fruit) with missing expiry & batch
        List<Inventory> nullExpiryList = inventoryRepository.findAll().stream()
                .filter(i -> i.getVariant() != null && i.getVariant().getProduct() != null
                        && Boolean.TRUE.equals(i.getVariant().getProduct().getIsPerishable())
                        && i.getExpiryDate() == null)
                .toList();
        for (Inventory inv : nullExpiryList) {
            inv.setIsPerishable(true);
            inv.setExpiryDate(java.time.LocalDate.of(2027, 2, 4));
            inv.setBatchNumber("F01");
            inventoryRepository.save(inv);
        }
    }

    private Permission createPermissionIfNotFound(String name) {
        return permissionRepository.findByPermissionName(name).orElseGet(() -> {
            return permissionRepository.save(Permission.builder().permissionName(name).build());
        });
    }
}