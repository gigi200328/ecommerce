package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "parent_id", referencedColumnName = "category_id", nullable = true, foreignKey = @ForeignKey(name = "categories_ibfk_1"))
    private Category parent;

    @Column(name = "category_name", nullable = false, length = 120)
    private String categoryName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_categories_users1"))
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modified_by", referencedColumnName = "user_id", foreignKey = @ForeignKey(name = "fk_categories_users2"))
    private User modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
