package com.realestate.backend.controller;

import com.realestate.backend.service.SavedPropertyService;
import com.realestate.backend.service.SavedPropertyService.SavedPropertyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/properties/saved")
@RequiredArgsConstructor
@Tag(name = "Saved Properties")
@SecurityRequirement(name = "BearerAuth")
public class SavedPropertyController {

    private final SavedPropertyService savedService;

    // ── GET /api/properties/saved — lista e të ruajturave ────────
    @GetMapping
    @Operation(summary = "Pronat e ruajtura të userit aktual")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<SavedPropertyResponse>> getMySaved(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(
                savedService.getMySaved(PageRequest.of(page, size))
        );
    }

    // ── POST /api/properties/saved/{propertyId} — ruaj ───────────
    @PostMapping("/{propertyId}")
    @Operation(summary = "Ruaj pronën në listën e të preferuarave")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<SavedPropertyResponse> save(
            @PathVariable Long propertyId,
            @RequestParam(required = false) String note
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedService.save(propertyId, note));
    }

    // ── DELETE /api/properties/saved/{propertyId} — hiq ──────────
    @DeleteMapping("/{propertyId}")
    @Operation(summary = "Hiq pronën nga të ruajturat")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Void> unsave(@PathVariable Long propertyId) {
        savedService.unsave(propertyId);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/properties/saved/{propertyId}/check ─────────────
    @GetMapping("/{propertyId}/check")
    @Operation(summary = "A është e ruajtur kjo pronë nga useri aktual?")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Boolean> isSaved(@PathVariable Long propertyId) {
        return ResponseEntity.ok(savedService.isSaved(propertyId));
    }
}