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

import com.ojt.ecommerce.backofficeinventory.service.VariationOptionService;
import com.ojt.ecommerce.entity.VariationOption;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/variation-options")
@RequiredArgsConstructor
@Tag(name = "Variation Option Management", description = "Back-Office APIs for managing variation options (e.g. Red, Blue, XL)")
public class VariationOptionController {

    private final VariationOptionService variationOptionService;

    // CREATE
    @Operation(summary = "Create variation option", description = "Creates a new option value for a variation (e.g. 'Red' for 'Color').")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation option created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VariationOption.class),
                examples = @ExampleObject(name = "Created Option Example", value = """
                    {
                      "optionId": 1,
                      "variation": {
                        "variationId": 1,
                        "name": "Color"
                      },
                      "value": "Red"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Duplicate option value or Variation not found")
    })
    @PostMapping
    public VariationOption createVariationOption(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Variation option to create",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = VariationOption.class),
                    examples = @ExampleObject(name = "Create Option Request", value = """
                        {
                          "variation": {
                            "variationId": 1
                          },
                          "value": "Red"
                        }
                        """)
                )
            )
            @RequestBody VariationOption variationOption) {

        return variationOptionService.createVariationOption(variationOption);
    }

    // GET ALL
    @Operation(summary = "Get all variation options", description = "Retrieves all variation options.")
    @ApiResponse(responseCode = "200", description = "List of variation options",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = VariationOption.class),
            examples = @ExampleObject(name = "Options List Example", value = """
                [
                  {
                    "optionId": 1,
                    "variation": {
                      "variationId": 1,
                      "name": "Color"
                    },
                    "value": "Red"
                  },
                  {
                    "optionId": 2,
                    "variation": {
                      "variationId": 1,
                      "name": "Color"
                    },
                    "value": "Blue"
                  }
                ]
                """)))
    @GetMapping
    public List<VariationOption> getAllVariationOptions() {
        return variationOptionService.getAllVariationOptions();
    }

    // GET BY ID
    @Operation(summary = "Get variation option by ID", description = "Retrieves a single variation option by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation option found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VariationOption.class),
                examples = @ExampleObject(name = "Single Option Example", value = """
                    {
                      "optionId": 1,
                      "variation": {
                        "variationId": 1,
                        "name": "Color"
                      },
                      "value": "Red"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Variation option not found")
    })
    @GetMapping("/{id}")
    public VariationOption getVariationOptionById(@PathVariable Long id) {
        return variationOptionService.getVariationOptionById(id);
    }

    // GET BY VARIATION ID
    @Operation(summary = "Get options by variation ID", description = "Retrieves all option values for a specific variation type.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List of options for variation",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VariationOption.class),
                examples = @ExampleObject(name = "Options for Variation Example", value = """
                    [
                      {
                        "optionId": 1,
                        "variation": {
                          "variationId": 1,
                          "name": "Color"
                        },
                        "value": "Red"
                      },
                      {
                        "optionId": 2,
                        "variation": {
                          "variationId": 1,
                          "name": "Color"
                        },
                        "value": "Blue"
                      }
                    ]
                    """))),
        @ApiResponse(responseCode = "500", description = "Variation not found")
    })
    @GetMapping("/variation/{variationId}")
    public List<VariationOption> getOptionsByVariationId(@PathVariable Long variationId) {
        return variationOptionService.getOptionsByVariationId(variationId);
    }

    // UPDATE
    @Operation(summary = "Update variation option", description = "Updates the value of an existing variation option.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation option updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = VariationOption.class),
                examples = @ExampleObject(name = "Updated Option Example", value = """
                    {
                      "optionId": 1,
                      "variation": {
                        "variationId": 1,
                        "name": "Color"
                      },
                      "value": "Crimson Red"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Variation option not found or duplicate value")
    })
    @PutMapping("/{id}")
    public VariationOption updateVariationOption(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated variation option details",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = VariationOption.class),
                    examples = @ExampleObject(name = "Update Option Request", value = """
                        {
                          "value": "Crimson Red"
                        }
                        """)
                )
            )
            @RequestBody VariationOption variationOption) {

        return variationOptionService.updateVariationOption(id, variationOption);
    }

    // DELETE
    @Operation(summary = "Delete variation option", description = "Deletes a variation option by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation option deleted successfully",
            content = @Content(mediaType = "text/plain",
                examples = @ExampleObject(name = "Delete Response", value = "Variation option deleted successfully."))),
        @ApiResponse(responseCode = "500", description = "Variation option not found")
    })
    @DeleteMapping("/{id}")
    public String deleteVariationOption(@PathVariable Long id) {
        variationOptionService.deleteVariationOption(id);
        return "Variation option deleted successfully.";
    }
}