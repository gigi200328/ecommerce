package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "permission_name", nullable = false, length = 60)
    private String permissionName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
