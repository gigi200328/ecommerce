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

import com.ojt.ecommerce.backofficeinventory.service.VariantOptionValueService;
import com.ojt.ecommerce.entity.VariantOptionValue;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/variant-option-values")
@RequiredArgsConstructor
@Tag(name = "Variant Option Value Management", description = "Back-Office APIs for mapping Product Variants to Variation Options")
public class VariantOptionValueController {

    private final VariantOptionValueService variantOptionValueService;

    // CREATE
    @Operation(
        summary = "Assign a variation option to a product variant",
        description = "Creates a mapping between a Product Variant and a Variation Option. Duplicate combinations are prevented."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mapping created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VariantOptionValue.class),
                examples = @ExampleObject(
                    name = "Created Mapping Example",
                    value = """
                        {
                          "id": 1,
                          "variant": {
                            "variantId": 1,
                            "sku": "NIKE-AIR-BLK-42",
                            "sellingPrice": 149.99,
                            "costPrice": 90.00,
                            "status": "ACTIVE"
                          },
                          "option": {
                            "optionId": 2,
                            "value": "Black"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Duplicate combination or entity not found")
    })
    @PostMapping
    public VariantOptionValue createVariantOptionValue(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Variant Option Value mapping payload",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VariantOptionValue.class),
                    examples = @ExampleObject(
                        name = "Create Mapping Request",
                        value = """
                            {
                              "variant": {
                                "variantId": 1
                              },
                              "option": {
                                "optionId": 2
                              }
                            }
                            """
                    )
                )
            )
            @RequestBody VariantOptionValue variantOptionValue) {

        return variantOptionValueService.createVariantOptionValue(variantOptionValue);
    }

    // GET ALL
    @Operation(
        summary = "Get all variant option mappings",
        description = "Retrieves all variant-option mapping records."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of all mappings",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VariantOptionValue.class),
                examples = @ExampleObject(
                    name = "Mappings List Example",
                    value = """
                        [
                          {
                            "id": 1,
                            "variant": {
                              "variantId": 1,
                              "sku": "NIKE-AIR-BLK-42",
                              "sellingPrice": 149.99,
                              "costPrice": 90.00,
                              "status": "ACTIVE"
                            },
                            "option": {
                              "optionId": 2,
                              "value": "Black"
                            }
                          },
                          {
                            "id": 2,
                            "variant": {
                              "variantId": 1,
                              "sku": "NIKE-AIR-BLK-42",
                              "sellingPrice": 149.99,
                              "costPrice": 90.00,
                              "status": "ACTIVE"
                            },
                            "option": {
                              "optionId": 5,
                              "value": "Size 42"
                            }
                          }
                        ]
                        """
                )
            )
        )
    })
    @GetMapping
    public List<VariantOptionValue> getAllVariantOptionValues() {
        return variantOptionValueService.getAllVariantOptionValues();
    }

    // GET BY ID
    @Operation(
        summary = "Get mapping by ID",
        description = "Retrieves a single variant-option mapping record by its primary key ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mapping found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VariantOptionValue.class),
                examples = @ExampleObject(
                    name = "Single Mapping Example",
                    value = """
                        {
                          "id": 1,
                          "variant": {
                            "variantId": 1,
                            "sku": "NIKE-AIR-BLK-42",
                            "sellingPrice": 149.99,
                            "costPrice": 90.00,
                            "status": "ACTIVE"
                          },
                          "option": {
                            "optionId": 2,
                            "value": "Black"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Mapping not found")
    })
    @GetMapping("/{id}")
    public VariantOptionValue getVariantOptionValueById(@PathVariable Long id) {
        return variantOptionValueService.getVariantOptionValueById(id);
    }

    // GET OPTIONS BY VARIANT ID
    @Operation(
        summary = "Get all options for a specific product variant",
        description = "Retrieves all variation options associated with a given product variant (e.g. all colors, sizes for this variant)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of options for the specified variant",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VariantOptionValue.class),
                examples = @ExampleObject(
                    name = "Options by Variant Example",
                    value = """
                        [
                          {
                            "id": 1,
                            "variant": {
                              "variantId": 1,
                              "sku": "NIKE-AIR-BLK-42",
                              "sellingPrice": 149.99,
                              "costPrice": 90.00,
                              "status": "ACTIVE"
                            },
                            "option": {
                              "optionId": 2,
                              "value": "Black"
                            }
                          },
                          {
                            "id": 2,
                            "variant": {
                              "variantId": 1,
                              "sku": "NIKE-AIR-BLK-42",
                              "sellingPrice": 149.99,
                              "costPrice": 90.00,
                              "status": "ACTIVE"
                            },
                            "option": {
                              "optionId": 5,
                              "value": "Size 42"
                            }
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Product variant not found")
    })
    @GetMapping("/variant/{variantId}")
    public List<VariantOptionValue> getOptionsByVariantId(@PathVariable Long variantId) {
        return variantOptionValueService.getOptionsByVariantId(variantId);
    }

    // GET VARIANTS BY OPTION ID
    @Operation(
        summary = "Get all variants using a specific variation option",
        description = "Retrieves all product variants that have a given variation option assigned."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "List of variants using the specified option",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VariantOptionValue.class),
                examples = @ExampleObject(
                    name = "Variants by Option Example",
                    value = """
                        [
                          {
                            "id": 1,
                            "variant": {
                              "variantId": 1,
                              "sku": "NIKE-AIR-BLK-42",
                              "sellingPrice": 149.99,
                              "costPrice": 90.00,
                              "status": "ACTIVE"
                            },
                            "option": {
                              "optionId": 2,
                              "value": "Black"
                            }
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Variation option not found")
    })
    @GetMapping("/option/{optionId}")
    public List<VariantOptionValue> getVariantsByOptionId(@PathVariable Long optionId) {
        return variantOptionValueService.getVariantsByOptionId(optionId);
    }

    // UPDATE
    @Operation(
        summary = "Update an existing mapping",
        description = "Updates an existing variant-option mapping record while checking against duplicate combinations."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mapping updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VariantOptionValue.class),
                examples = @ExampleObject(
                    name = "Updated Mapping Example",
                    value = """
                        {
                          "id": 1,
                          "variant": {
                            "variantId": 1,
                            "sku": "NIKE-AIR-BLK-42",
                            "sellingPrice": 149.99,
                            "costPrice": 90.00,
                            "status": "ACTIVE"
                          },
                          "option": {
                            "optionId": 3,
                            "value": "White"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Mapping not found or duplicate combination")
    })
    @PutMapping("/{id}")
    public VariantOptionValue updateVariantOptionValue(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated mapping details",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VariantOptionValue.class),
                    examples = @ExampleObject(
                        name = "Update Mapping Request",
                        value = """
                            {
                              "variant": {
                                "variantId": 1
                              },
                              "option": {
                                "optionId": 3
                              }
                            }
                            """
                    )
                )
            )
            @RequestBody VariantOptionValue variantOptionValue) {

        return variantOptionValueService.updateVariantOptionValue(id, variantOptionValue);
    }

    // DELETE
    @Operation(
        summary = "Delete mapping by ID",
        description = "Deletes a variant-option mapping record by its primary key ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Mapping deleted successfully",
            content = @Content(
                mediaType = "text/plain",
                examples = @ExampleObject(
                    name = "Delete Success Response",
                    value = "Variant option mapping deleted successfully."
                )
            )
        ),
        @ApiResponse(responseCode = "500", description = "Mapping not found")
    })
    @DeleteMapping("/{id}")
    public String deleteVariantOptionValue(@PathVariable Long id) {
        variantOptionValueService.deleteVariantOptionValue(id);
        return "Variant option mapping deleted successfully.";
    }
}
