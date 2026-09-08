package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variation_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variation_id", referencedColumnName = "variation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_option_variation"))
    private Variation variation;

    @Column(name = "value", nullable = false, length = 150)
    private String value;
}
