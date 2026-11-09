package com.ojt.ecommerce.storefront.auth.repository;

import com.ojt.ecommerce.entity.Customer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByEmail(String email);

	boolean existsByEmail(String email);

	// Case-insensitive ဖြင့် Email ရှာဖွေရန်
	Optional<Customer> findByEmailIgnoreCase(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM Customer c WHERE c.customerId = :id")
	Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}
