package com.ojt.ecommerce.storefront.repository;
import com.ojt.ecommerce.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerCustomerId(Long customerId);
    Optional<CustomerAddress> findByAddressIdAndCustomerCustomerId(Long addressId, Long customerId);
    long countByCustomerCustomerId(Long customerId);
}