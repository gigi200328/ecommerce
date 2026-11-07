package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u JOIN FETCH u.userRole WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
    boolean existsByEmailIgnoreCase(@Param("email") String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.userRole.roleName = :roleName AND u.status = :status")
    long countByRoleNameAndStatus(@Param("roleName") String roleName, @Param("status") String status);
}
