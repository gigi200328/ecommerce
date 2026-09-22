package com.ojt.ecommerce.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "variant_option_values",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_variant_option_combination", columnNames = {"variant_id", "option_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Mapping entity associating a Product Variant with a Variation Option")
public class VariantOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Schema(description = "Primary Key (Auto Increment)", example = "1")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", referencedColumnName = "variant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapping_variant"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "product"})
    @Schema(description = "Associated Product Variant")
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", referencedColumnName = "option_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapping_option"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "variation"})
    @Schema(description = "Associated Variation Option (e.g. Red, XL)")
    private VariationOption option;
}
