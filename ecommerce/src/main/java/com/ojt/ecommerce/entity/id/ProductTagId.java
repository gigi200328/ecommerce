package com.ojt.ecommerce.entity.id;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTagId implements Serializable {
    private Long product;
    private Long tag;
}
