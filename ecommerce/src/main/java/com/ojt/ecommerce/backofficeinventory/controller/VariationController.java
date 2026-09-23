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

import com.ojt.ecommerce.backofficeinventory.service.VariationService;
import com.ojt.ecommerce.entity.Variation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/variations")
@RequiredArgsConstructor
@Tag(name = "Variation Management", description = "Back-Office APIs for managing product variation types (e.g. Color, Size)")
public class VariationController {

    private final VariationService variationService;

    // CREATE
    @Operation(summary = "Create variation", description = "Creates a new variation attribute like Color or Size.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation created successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Variation.class),
                examples = @ExampleObject(name = "Created Variation", value = """
                    {
                      "variationId": 1,
                      "name": "Color"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Duplicate variation name")
    })
    @PostMapping
    public Variation createVariation(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Variation to create",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = Variation.class),
                    examples = @ExampleObject(name = "Create Variation Request", value = """
                        {
                          "name": "Color"
                        }
                        """)
                )
            )
            @RequestBody Variation variation) {
        return variationService.createVariation(variation);
    }

    // GET ALL
    @Operation(summary = "Get all variations", description = "Retrieves all variations.")
    @ApiResponse(responseCode = "200", description = "List of variations",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Variation.class),
            examples = @ExampleObject(name = "Variation List", value = """
                [
                  {
                    "variationId": 1,
                    "name": "Color"
                  },
                  {
                    "variationId": 2,
                    "name": "Size"
                  }
                ]
                """)))
    @GetMapping
    public List<Variation> getAllVariations() {
        return variationService.getAllVariations();
    }

    // GET BY ID
    @Operation(summary = "Get variation by ID", description = "Retrieves a single variation by its ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation found",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Variation.class),
                examples = @ExampleObject(name = "Single Variation", value = """
                    {
                      "variationId": 1,
                      "name": "Color"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Variation not found")
    })
    @GetMapping("/{id}")
    public Variation getVariationById(@PathVariable Long id) {
        return variationService.getVariationById(id);
    }

    // UPDATE
    @Operation(summary = "Update variation", description = "Updates a variation name by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation updated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Variation.class),
                examples = @ExampleObject(name = "Updated Variation", value = """
                    {
                      "variationId": 1,
                      "name": "Colour"
                    }
                    """))),
        @ApiResponse(responseCode = "500", description = "Variation not found")
    })
    @PutMapping("/{id}")
    public Variation updateVariation(
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated variation payload",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = Variation.class),
                    examples = @ExampleObject(name = "Update Variation Request", value = """
                        {
                          "name": "Colour"
                        }
                        """)
                )
            )
            @RequestBody Variation variation) {
        return variationService.updateVariation(id, variation);
    }

    // DELETE
    @Operation(summary = "Delete variation", description = "Deletes a variation by ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variation deleted successfully",
            content = @Content(mediaType = "text/plain",
                examples = @ExampleObject(name = "Delete Response", value = "Variation deleted successfully."))),
        @ApiResponse(responseCode = "500", description = "Variation not found")
    })
    @DeleteMapping("/{id}")
    public String deleteVariation(@PathVariable Long id) {
        variationService.deleteVariation(id);
        return "Variation deleted successfully.";
    }
}