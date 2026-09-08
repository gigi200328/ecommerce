package com.ojt.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variation_id", nullable = false)
    private Long variationId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
