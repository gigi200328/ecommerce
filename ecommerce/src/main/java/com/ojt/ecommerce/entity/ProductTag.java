package com.ojt.ecommerce.entity;

import com.ojt.ecommerce.entity.id.ProductTagId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_tags")
@IdClass(ProductTagId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTag {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_tag_product"))
    private Product product;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", referencedColumnName = "tag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_product_tag_tag"))
    private Tag tag;
}
