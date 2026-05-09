package com.realestate.backend.controller;

import com.realestate.backend.dto.property.PropertyDtos.PropertyImageResponse;
import com.realestate.backend.entity.property.PropertyImage;
import com.realestate.backend.service.PropertyImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/properties/{propertyId}/images")
@RequiredArgsConstructor
@Tag(name = "Property Images")
@SecurityRequirement(name = "BearerAuth")
public class PropertyImageController extends BaseController {

    private final PropertyImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ngarko imazh për pronën")
    public ResponseEntity<PropertyImageResponse> upload(
            @PathVariable Long propertyId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String caption,
            @RequestParam(defaultValue = "false") boolean primary
    ) throws IOException {
        System.out.println("UPLOAD HIT CONTROLLER");
        return created(toResponse(imageService.uploadImage(propertyId, file, caption, primary)));
    }

    @GetMapping
    @Operation(summary = "Merr të gjitha imazhet e pronës")
    public ResponseEntity<List<PropertyImageResponse>> getImages(
            @PathVariable Long propertyId) {
        return ok(imageService.getImages(propertyId).stream()
                .map(this::toResponse).toList());
    }

    @PatchMapping("/{imageId}/primary")
    @Operation(summary = "Vendos imazhin si kryesor (primary)")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        imageService.setPrimary(propertyId, imageId);
        return noContent();
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Fshij imazhin")
    public ResponseEntity<Void> delete(
            @PathVariable Long propertyId,
            @PathVariable Long imageId) {
        imageService.deleteImage(propertyId, imageId);
        return noContent();
    }

    private PropertyImageResponse toResponse(PropertyImage i) {
        return new PropertyImageResponse(
                i.getId(), i.getImageUrl(),
                i.getCaption(), i.getSortOrder(), i.getIsPrimary()
        );
    }
}