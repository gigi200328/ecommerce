
package com.ojt.ecommerce.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "categories",
    indexes = {
        @Index(
            name = "idx_category_parent_id",
            columnList = "parent_id"
        )
    }
)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "parent_id",
        referencedColumnName = "category_id",
        nullable = true,
        foreignKey = @ForeignKey(
            name = "fk_categories_parent"
        )
    )
    private Category parent;

    @Column(
        name = "category_name",
        nullable = false,
        length = 120
    )
    private String categoryName;

    @Column(
        name = "description",
        columnDefinition = "TEXT"
    )
    private String description;

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "created_by",
        referencedColumnName = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "fk_categories_created_by"
        )
    )
    private User createdBy;

    @Column(
        name = "created_at",
        nullable = false
    )
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "modified_by",
        referencedColumnName = "user_id",
        foreignKey = @ForeignKey(
            name = "fk_categories_modified_by"
        )
    )
    private User modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
        mappedBy = "category",
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<BrandCategory> brandCategories = new HashSet<>();
}

