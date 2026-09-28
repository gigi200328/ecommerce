package com.ojt.ecommerce.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Variation Attribute Entity (e.g. Color, Size, Storage)")
public class Variation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variation_id", nullable = false)
    @Schema(description = "Primary Key (Auto Increment)", example = "1")
    private Long variationId;

    @Column(name = "name", nullable = false, length = 100)
    @Schema(description = "Variation name (e.g. Color, Size)", example = "Color")
    private String name;
}
