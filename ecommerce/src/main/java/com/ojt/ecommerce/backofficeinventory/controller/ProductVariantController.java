package com.ojt.ecommerce.backofficeinventory.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ojt.ecommerce.backofficeinventory.service.ProductVariantService;
import com.ojt.ecommerce.entity.ProductVariant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/product-variants")
@RequiredArgsConstructor
@Tag(name = "Product Variant Management", description = "Back-Office APIs for managing product variants")
public class ProductVariantController {

    private final ProductVariantService productVariantService;

    // CREATE
    @Operation(
        summary = "Create a new product variant",
        description = "Creates a new product variant with a unique SKU and pricing details."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product variant created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductVariant.class),
                examples = @ExampleObject(
                    name = "Created Variant Example",
                    value = """
                        {
                          "variantId": 1,
                          "product": {
                            "productId": 1,
                            "productName": "Nike Air Max"
                          },
                          "sku": "NIKE-AIR-BLK-42",
                          "sellingPrice": 149.99,
                          "costPrice": 90.00,
                          "status": "ACTIVE"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Product not found or duplicate SKU")
    })
    @PostMapping
    public ProductVariant createProductVariant(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Product variant payload to create",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductVariant.class),
                    examples = @ExampleObject(
                        name = "Create Variant Request",
                        value = """
                            {
                              "product": {
                                "productId": 1
                              },
                              "sku": "NIKE-AIR-BLK-42",
                              "sellingPrice": 149.99,
                              "costPrice": 90.00,
                              "status": "ACTIVE"
                            }
                            """
                    )
                )
            )
            @RequestBody ProductVariant productVariant) {

        return productVariantService.createProductVariant(productVariant);
    }

    // GET ALL
    @Operation(
        summary = "Get all product variants",
        description = "Retrieves all product variants in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of product variants",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductVariant.class),
                examples = @ExampleObject(
                    name = "Variant List Example",
                    value = """
                        [
                          {
                            "variantId": 1,
                            "product": {
                              "productId": 1,
                              "productName": "Nike Air Max"
                            },
                            "sku": "NIKE-AIR-BLK-42",
                            "sellingPrice": 149.99,
                            "costPrice": 90.00,
                            "status": "ACTIVE"
                          }
                        ]
                        """
                )
            )
        )
    })
    @GetMapping
    public List<ProductVariant> getAllProductVariants() {
        return productVariantService.getAllProductVariants();
    }

    // GET BY ID
    @Operation(
        summary = "Get product variant by ID",
        description = "Retrieves a single product variant by its primary key ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product variant found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductVariant.class),
                examples = @ExampleObject(
                    name = "Single Variant Example",
                    value = """
                        {
                          "variantId": 1,
                          "product": {
                            "productId": 1,
                            "productName": "Nike Air Max"
                          },
                          "sku": "NIKE-AIR-BLK-42",
                          "sellingPrice": 149.99,
                          "costPrice": 90.00,
                          "status": "ACTIVE"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Product variant not found")
    })
    @GetMapping("/{id}")
    public ProductVariant getProductVariantById(@PathVariable Long id) {
        return productVariantService.getProductVariantById(id);
    }

    // GET BY PRODUCT ID
    @Operation(
        summary = "Get variants by product ID",
        description = "Retrieves all product variants that belong to a specific product."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of variants for the given product",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductVariant.class),
                examples = @ExampleObject(
                    name = "Variants by Product Example",
                    value = """
                        [
                          {
                            "variantId": 1,
                            "product": {
                              "productId": 1,
                              "productName": "Nike Air Max"
                            },
                            "sku": "NIKE-AIR-BLK-42",
                            "sellingPrice": 149.99,
                            "costPrice": 90.00,
                            "status": "ACTIVE"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Product not found")
    })
    @GetMapping("/product/{productId}")
    public List<ProductVariant> getVariantsByProductId(@PathVariable Long productId) {
        return productVariantService.getVariantsByProductId(productId);
    }

    // UPDATE
    @Operation(
        summary = "Update an existing product variant",
        description = "Updates details of a product variant by ID while enforcing SKU uniqueness."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product variant updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ProductVariant.class),
                examples = @ExampleObject(
                    name = "Updated Variant Example",
                    value = """
                        {
                          "variantId": 1,
                          "product": {
                            "productId": 1,
                            "productName": "Nike Air Max"
                          },
                          "sku": "NIKE-AIR-BLK-42-V2",
                          "sellingPrice": 159.99,
                          "costPrice": 95.00,
                          "status": "ACTIVE"
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Product variant not found or duplicate SKU")
    })
    @PutMapping("/{id}")
    public ProductVariant updateProductVariant(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated product variant details",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProductVariant.class),
                    examples = @ExampleObject(
                        name = "Update Variant Request",
                        value = """
                            {
                              "product": {
                                "productId": 1
                              },
                              "sku": "NIKE-AIR-BLK-42-V2",
                              "sellingPrice": 159.99,
                              "costPrice": 95.00,
                              "status": "ACTIVE"
                            }
                            """
                    )
                )
            )
            @RequestBody ProductVariant productVariant) {

        return productVariantService.updateProductVariant(id, productVariant);
    }

    // DELETE
    @Operation(
        summary = "Delete product variant",
        description = "Deletes a product variant by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Product variant deleted successfully",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(
                    name = "Delete Success Response",
                    value = "Product variant deleted successfully."
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Product variant not found")
    })
    @DeleteMapping("/{id}")
    public String deleteProductVariant(@PathVariable Long id) {
        productVariantService.deleteProductVariant(id);
        return "Product variant deleted successfully.";
    }
}
