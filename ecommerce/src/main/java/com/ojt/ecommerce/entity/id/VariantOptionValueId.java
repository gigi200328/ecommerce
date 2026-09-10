package com.ojt.ecommerce.entity.id;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantOptionValueId implements Serializable {
    private Long variant;
    private Long option;
}
