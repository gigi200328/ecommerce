package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // လိုအပ်ပါက အချိန်အလိုက်၊ User အလိုက် ရှာဖွေနိုင်သော query များ ဤနေရာတွင် ထပ်ရေးနိုင်ပါသည်
}