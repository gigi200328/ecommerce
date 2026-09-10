package com.ojt.ecommerce.entity;

import com.ojt.ecommerce.entity.id.VariantOptionValueId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_option_values")
@IdClass(VariantOptionValueId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantOptionValue {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", referencedColumnName = "variant_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapping_variant"))
    private ProductVariant variant;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", referencedColumnName = "option_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapping_option"))
    private VariationOption option;
}
