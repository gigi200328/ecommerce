package com.ojt.ecommerce.entity;

import java.time.LocalDateTime;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "product_images",
    indexes = {
        @Index(name = "idx_product_images_product_id", columnList = "product_id"),
        @Index(name = "idx_product_images_product_primary", columnList = "product_id, is_primary")
    }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false, foreignKey = @ForeignKey(name = "product_images_ibfk_1"))
    private Product product;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
    
    @Builder.Default
    @Column(name = "is_primary", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isPrimary = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_images_users1"))
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "modified_by",referencedColumnName = "user_id",nullable = false,foreignKey = @ForeignKey(name = "fk_product_images_users2")
    )
    private User modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}
