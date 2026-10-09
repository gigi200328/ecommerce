
package com.ojt.ecommerce.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.ojt.ecommerce.enums.BrandStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    @Column(name = "brand_logo_url", length = 500)
    private String brandLogoUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "ENUM('ACTIVE', 'INACTIVE')"
    )
    private BrandStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "created_by",
        referencedColumnName = "user_id",
        foreignKey = @ForeignKey(name = "fk_brands_created_by")
    )
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "modified_by",
        referencedColumnName = "user_id",
        foreignKey = @ForeignKey(name = "fk_brands_modified_by")
    )
    private User modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @OneToMany(
        mappedBy = "brand",
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<BrandCategory> brandCategories = new HashSet<>();
}
