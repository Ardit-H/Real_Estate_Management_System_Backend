package com.realestate.backend.controller;

import com.realestate.backend.dto.lead.LeadDtos.*;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Property Lead Requests")
@SecurityRequirement(name = "BearerAuth")
public class LeadController {

    private final LeadService leadService;

    // ── pa ndryshim ───────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listo leads sipas statusit (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<LeadResponse>> getByStatus(
            @RequestParam(defaultValue = "NEW") LeadStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                leadService.getByStatus(status, PageRequest.of(page, size))
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr lead sipas ID")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<LeadResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.getById(id));
    }

    @GetMapping("/my/agent")
    @Operation(summary = "Leads e asinjuara tek unë (AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<Page<LeadResponse>> getMyLeadsAsAgent(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                leadService.getMyLeadsAsAgent(PageRequest.of(page, size))
        );
    }

    @GetMapping("/my/client")
    @Operation(summary = "Kërkesat e mia si klient")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<Page<LeadResponse>> getMyLeadsAsClient(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                leadService.getMyLeadsAsClient(PageRequest.of(page, size))
        );
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Leads të paasinjuara — NEW pa agjent (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeadResponse>> getUnassigned() {
        return ResponseEntity.ok(leadService.getUnassigned());
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Leads sipas pronës (ADMIN/AGENT)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<List<LeadResponse>> getByProperty(@PathVariable Long propertyId) {
        return ResponseEntity.ok(leadService.getByProperty(propertyId));
    }

    @PostMapping
    @Operation(summary = "Krijo kërkesë të re (çdo rol)")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<LeadResponse> create(
            @Valid @RequestBody LeadCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leadService.create(request));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Asinjono agjent — statusi MBETET NEW, agjenti pranon vetë (ADMIN)")
    // NDRYSHIM dokumentimi: assignAgent tani nuk e kalon IN_PROGRESS automatikisht
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeadResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody LeadAssignRequest request) {
        return ResponseEntity.ok(leadService.assignAgent(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin — NEW→IN_PROGRESS (Accept), IN_PROGRESS→DONE/REJECTED (ADMIN/AGENT)")
    // NDRYSHIM: agjenti mund të ndryshojë vetëm leads të asignuara tek ai
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<LeadResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeadStatusRequest request) {
        return ResponseEntity.ok(leadService.updateStatus(id, request));
    }

    // ── SHTUAR: endpoint i ri për Decline ─────────────────────────────────────
    // Agjenti refuzon operacionalisht — lead kthehet tek admini si NEW pa agjent
    // E ndryshme nga REJECTED (final biznesi) — DECLINE rifillon ciklin
    @PatchMapping("/{id}/decline")
    @Operation(
            summary = "Agjenti refuzon lead-in (Decline) — kthehet tek admini si NEW pa agjent",
            description = "Përdoret kur agjenti nuk mund ta trajtojë lead-in (është i zënë, nuk i përshtatet). " +
                    "Lead kthehet si NEW pa agjent — admini e sheh në /unassigned dhe e assign tek tjetri. " +
                    "E NDRYSHME nga REJECTED: REJECTED = vendim final biznesi, DECLINE = refuzim operacional."
    )
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<LeadResponse> declineLead(@PathVariable Long id) {
        return ResponseEntity.ok(leadService.declineLead(id));
    }
}