package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    // Seeder ထဲတွင် ခေါ်သုံးထားသော findByPermissionName အတွက် Method
    Optional<Permission> findByPermissionName(String permissionName);
    
}