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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variation_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Variation Option Entity (e.g. Red, Blue, XL)")
public class VariationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id", nullable = false)
    @Schema(description = "Primary Key (Auto Increment)", example = "1")
    private Long optionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variation_id", referencedColumnName = "variation_id", nullable = false, foreignKey = @ForeignKey(name = "fk_option_variation"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @Schema(description = "Associated Variation")
    private Variation variation;

    @Column(name = "value", nullable = false, length = 150)
    @Schema(description = "Option value name", example = "Black")
    private String value;
}
