package com.ojt.ecommerce.storefront.auth.repository;
import com.ojt.ecommerce.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
}