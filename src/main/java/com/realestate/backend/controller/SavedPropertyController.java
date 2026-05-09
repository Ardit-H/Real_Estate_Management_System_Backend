package com.realestate.backend.controller;

import com.realestate.backend.service.SavedPropertyService;
import com.realestate.backend.service.SavedPropertyService.SavedPropertyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties/saved")
@RequiredArgsConstructor
@Tag(name = "Saved Properties")
@SecurityRequirement(name = "BearerAuth")
public class SavedPropertyController extends BaseController {

    private final SavedPropertyService savedService;

    @GetMapping
    @Operation(summary = "Pronat e ruajtura të userit aktual")
    public ResponseEntity<Page<SavedPropertyResponse>> getMySaved(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ok(savedService.getMySaved(page(page, size)));
    }

    @PostMapping("/{propertyId}")
    @Operation(summary = "Ruaj pronën në listën e të preferuarave")
    public ResponseEntity<SavedPropertyResponse> save(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String note
    ) {
        return created(savedService.save(propertyId, note));
    }

    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Hiq pronën nga të ruajturat")
    public ResponseEntity<Void> unsave(@PathVariable Long propertyId) {
        savedService.unsave(propertyId);
        return noContent();
    }

    @GetMapping("/{propertyId}/check")
    @Operation(summary = "A është e ruajtur kjo pronë nga useri aktual?")
    public ResponseEntity<Boolean> isSaved(@PathVariable Long propertyId) {
        return ok(savedService.isSaved(propertyId));
    }
}