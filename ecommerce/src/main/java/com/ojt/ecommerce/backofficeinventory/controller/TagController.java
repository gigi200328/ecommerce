package com.ojt.ecommerce.backofficeinventory.controller;

import com.ojt.ecommerce.backofficeinventory.dto.TagRequestDto;
import com.ojt.ecommerce.backofficeinventory.dto.TagResponseDto;
import com.ojt.ecommerce.backofficeinventory.service.TagService;

// Audit Log အတွက် အသစ်ထည့်ထားသော Import များ
import com.ojt.ecommerce.annotation.Auditable;
import com.ojt.ecommerce.entity.AuditAction;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backoffice/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * Create tag.
     */
    @Auditable(action = AuditAction.CREATE, moduleName = "Tag Management")
    @PostMapping
    public ResponseEntity<TagResponseDto> createTag(
            @Valid @RequestBody TagRequestDto request
    ) {

        return new ResponseEntity<>(
                tagService.createTag(request),
                HttpStatus.CREATED
        );
    }

    /**
     * Get tags with pagination, sorting and search.
     */
    @GetMapping
    public ResponseEntity<Page<TagResponseDto>> getAllTags(

            @RequestParam(
                    required = false,
                    defaultValue = ""
            )
            String search,

            @PageableDefault(
                    size = 20,
                    sort = "tagName"
            )
            Pageable pageable

    ) {

        return ResponseEntity.ok(
                tagService.getAllTags(
                        search,
                        pageable
                )
        );
    }

    /**
     * Get tag by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TagResponseDto> getTagById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                tagService.getTagById(id)
        );
    }

    /**
     * Update tag.
     */
    @Auditable(action = AuditAction.UPDATE, moduleName = "Tag Management")
    @PutMapping("/{id}")
    public ResponseEntity<TagResponseDto> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagRequestDto request
    ) {

        return ResponseEntity.ok(
                tagService.updateTag(
                        id,
                        request
                )
        );
    }

    /**
     * Delete tag.
     */
    @Auditable(action = AuditAction.DELETE, moduleName = "Tag Management")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(
            @PathVariable Long id
    ) {

        tagService.deleteTag(id);

        return ResponseEntity.noContent().build();
    }
}