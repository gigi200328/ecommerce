package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
