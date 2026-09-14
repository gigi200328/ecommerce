package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    
    @Query("SELECT u FROM User u JOIN FETCH u.userRole WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
    
}