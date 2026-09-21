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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/variation-options")
@RequiredArgsConstructor
public class VariationOptionController {

    private final VariationOptionService variationOptionService;

    // CREATE
    @PostMapping
    public VariationOption createVariationOption(
            @RequestBody VariationOption variationOption) {

        return variationOptionService
                .createVariationOption(variationOption);
    }

    // GET ALL
    @GetMapping
    public List<VariationOption> getAllVariationOptions() {

        return variationOptionService
                .getAllVariationOptions();
    }

    // GET BY ID
    @GetMapping("/{id}")
    public VariationOption getVariationOptionById(
            @PathVariable Long id) {

        return variationOptionService
                .getVariationOptionById(id);
    }

    // GET BY VARIATION ID
    @GetMapping("/variation/{variationId}")
    public List<VariationOption> getOptionsByVariationId(
            @PathVariable Long variationId) {

        return variationOptionService
                .getOptionsByVariationId(variationId);
    }

    // UPDATE
    @PutMapping("/{id}")
    public VariationOption updateVariationOption(
            @PathVariable Long id,
            @RequestBody VariationOption variationOption) {

        return variationOptionService
                .updateVariationOption(id, variationOption);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public String deleteVariationOption(
            @PathVariable Long id) {

        variationOptionService
                .deleteVariationOption(id);

        return "Variation option deleted successfully.";
    }
}