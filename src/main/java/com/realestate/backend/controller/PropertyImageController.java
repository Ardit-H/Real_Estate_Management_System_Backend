package com.realestate.backend.controller;

import com.realestate.backend.dto.property.PropertyDtos.PropertyImageResponse;
import com.realestate.backend.entity.property.PropertyImage;
import com.realestate.backend.service.PropertyImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/images")
@RequiredArgsConstructor
@Tag(name = "Property Images")
@SecurityRequirement(name = "BearerAuth")
public class PropertyImageController {

    private final PropertyImageService imageService;

    // ── 1. POST /api/properties/{id}/images — ngarko imazh ───────
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ngarko imazh për pronën")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PropertyImageResponse> upload(
            @PathVariable Long propertyId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(defaultValue = "false") boolean primary
    ) throws IOException {
        System.out.println("👉 UPLOAD HIT CONTROLLER");
        PropertyImage img = imageService.uploadImage(propertyId, file, caption, primary);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(img));
    }

    // ── 2. GET /api/properties/{id}/images — lista ─────────────
    @GetMapping
    @Operation(summary = "Merr të gjitha imazhet e pronës")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<List<PropertyImageResponse>> getImages(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(
                imageService.getImages(propertyId).stream()
                        .map(this::toResponse).toList()
        );
    }

    // ── 3. PATCH /api/properties/{id}/images/{imageId}/primary ──
    @PatchMapping("/{imageId}/primary")
    @Operation(summary = "Vendos imazhin si kryesor (primary)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        imageService.setPrimary(propertyId, imageId);
        return ResponseEntity.noContent().build();
    }

    // ── 4. DELETE /api/properties/{id}/images/{imageId} ─────────
    @DeleteMapping("/{imageId}")
    @Operation(summary = "Fshij imazhin")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Void> delete(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        imageService.deleteImage(propertyId, imageId);
        return ResponseEntity.noContent().build();
    }


    private PropertyImageResponse toResponse(PropertyImage i) {
        return new PropertyImageResponse(
                i.getId(), i.getImageUrl(),
                i.getCaption(), i.getSortOrder(), i.getIsPrimary()
        );
    }
}