package com.ojt.ecommerce.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "product_variants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_variant_sku", columnNames = {"sku"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Product Variant Entity representing a specific sellable variant of a product")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id", nullable = false)
    @Schema(description = "Primary Key (Auto Increment)", example = "1")
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_variant_product"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "createdBy", "modifiedBy"})
    @Schema(description = "Associated Product")
    private Product product;

    @Column(name = "sku", nullable = false, unique = true, length = 100)
    @Schema(description = "Stock Keeping Unit (Unique, Not Null)", example = "IPHONE-15-PRO-BLK-128")
    private String sku;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    @Schema(description = "Selling price of the variant", example = "999.99")
    private BigDecimal sellingPrice;

    @Column(name = "cost_price", precision = 12, scale = 2)
    @Schema(description = "Cost price of the variant", example = "750.00")
    private BigDecimal costPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'INACTIVE')")
    @Schema(description = "Variant status", example = "ACTIVE")
    private ProductVariantStatus status;
}
