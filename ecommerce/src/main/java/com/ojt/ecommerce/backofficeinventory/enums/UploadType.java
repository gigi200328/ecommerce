package com.ojt.ecommerce.backofficeinventory.enums;

import lombok.Getter;

@Getter
public enum UploadType {
	PRODUCT_IMAGE("product-images"),
    BRAND_LOGO("brand-logos");

    private final String directoryName;

    UploadType(String directoryName) {
        this.directoryName = directoryName;
    }

}
