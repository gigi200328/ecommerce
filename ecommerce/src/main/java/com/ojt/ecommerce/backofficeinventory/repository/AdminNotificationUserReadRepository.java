package com.ojt.ecommerce.backofficeinventory.repository;

import com.ojt.ecommerce.entity.AdminNotification;
import com.ojt.ecommerce.entity.AdminNotificationUserRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AdminNotificationUserReadRepository extends JpaRepository<AdminNotificationUserRead, Long> {

    boolean existsByNotificationIdAndUserId(Long notificationId, Long userId);

    @Query("SELECT r.notificationId FROM AdminNotificationUserRead r WHERE r.userId = :userId")
    Set<Long> findReadNotificationIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM AdminNotification n WHERE n.notificationId NOT IN (" +
           "SELECT r.notificationId FROM AdminNotificationUserRead r WHERE r.userId = :userId)")
    long countUnreadByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM AdminNotification n WHERE n.notificationId NOT IN (" +
           "SELECT r.notificationId FROM AdminNotificationUserRead r WHERE r.userId = :userId) ORDER BY n.createdAt DESC")
    List<AdminNotification> findUnreadByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
