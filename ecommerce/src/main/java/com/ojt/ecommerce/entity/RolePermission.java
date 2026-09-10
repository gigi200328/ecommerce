package com.ojt.ecommerce.entity;

import com.ojt.ecommerce.entity.id.RolePermissionId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", referencedColumnName = "role_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role_has_permissions_user_role1"))
    private Role role;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", referencedColumnName = "permission_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_role_has_permissions_permissions1"))
    private Permission permission;
}
