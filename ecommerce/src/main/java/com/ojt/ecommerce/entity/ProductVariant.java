package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_variant_product"))
    private Product product;

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "weight", precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "attributes", columnDefinition = "JSON")
    private String attributes;

    @Column(name = "status", nullable = false, columnDefinition = "ENUM('ACTIVE', 'INACTIVE')")
    private String status;

    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;
}
