package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Set; // Set ကို Import လုပ်ရန် လိုအပ်ပါသည်

@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "role_name", nullable = false, length = 45)
    private String roleName;

    // Role နှင့် Permission ကို ချိတ်ဆက်ပေးမည့် Many-To-Many ဆက်သွယ်ချက်
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions", // MySQL ထဲရှိ Mapping ဇယားအမည်
            joinColumns = @JoinColumn(name = "role_id"), // လက်ရှိ Role ဇယား၏ Foreign Key
            inverseJoinColumns = @JoinColumn(name = "permission_id") // ချိတ်ဆက်မည့် Permission ဇယား၏ Foreign Key
    )
    private Set<Permission> permissions;
}