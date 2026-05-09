package com.realestate.backend.controller;

import com.realestate.backend.dto.lead.LeadDtos.*;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Tag(name = "Property Lead Requests")
@SecurityRequirement(name = "BearerAuth")
public class LeadController extends BaseController {

    private final LeadService leadService;

    @GetMapping
    @Operation(summary = "Listo leads sipas statusit (ADMIN/AGENT)")
    public ResponseEntity<Page<LeadResponse>> getByStatus(
            @RequestParam(defaultValue = "NEW") LeadStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(leadService.getByStatus(status, page(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Merr lead sipas ID")
    public ResponseEntity<LeadResponse> getById(@PathVariable Long id) {
        return ok(leadService.getById(id));
    }

    @GetMapping("/my/agent")
    @Operation(summary = "Leads e asinjuara tek unë (AGENT)")
    public ResponseEntity<Page<LeadResponse>> getMyLeadsAsAgent(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(leadService.getMyLeadsAsAgent(page(page, size)));
    }

    @GetMapping("/my/client")
    @Operation(summary = "Kërkesat e mia si klient")
    public ResponseEntity<Page<LeadResponse>> getMyLeadsAsClient(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ok(leadService.getMyLeadsAsClient(page(page, size)));
    }

    @GetMapping("/unassigned")
    @Operation(summary = "Leads të paasinjuara — NEW pa agjent (ADMIN)")
    public ResponseEntity<List<LeadResponse>> getUnassigned() {
        return ok(leadService.getUnassigned());
    }

    @GetMapping("/property/{propertyId}")
    @Operation(summary = "Leads sipas pronës (ADMIN/AGENT)")
    public ResponseEntity<List<LeadResponse>> getByProperty(@PathVariable Long propertyId) {
        return ok(leadService.getByProperty(propertyId));
    }

    @PostMapping
    @Operation(summary = "Krijo kërkesë të re (çdo rol)")
    public ResponseEntity<LeadResponse> create(
            @Valid @RequestBody LeadCreateRequest request) {
        return created(leadService.create(request));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Asinjono agjent — statusi MBETET NEW, agjenti pranon vetë (ADMIN)")
    public ResponseEntity<LeadResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody LeadAssignRequest request) {
        return ok(leadService.assignAgent(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ndrysho statusin — NEW→IN_PROGRESS (Accept), IN_PROGRESS→DONE/REJECTED (ADMIN/AGENT)")
    public ResponseEntity<LeadResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeadStatusRequest request) {
        return ok(leadService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/decline")
    @Operation(
            summary = "Agjenti refuzon lead-in (Decline) — kthehet tek admini si NEW pa agjent",
            description = "Përdoret kur agjenti nuk mund ta trajtojë lead-in (është i zënë, nuk i përshtatet). " +
                    "Lead kthehet si NEW pa agjent — admini e sheh në /unassigned dhe e assign tek tjetri. " +
                    "E NDRYSHME nga REJECTED: REJECTED = vendim final biznesi, DECLINE = refuzim operacional."
    )
    public ResponseEntity<LeadResponse> declineLead(@PathVariable Long id) {
        return ok(leadService.declineLead(id));
    }

    @PatchMapping("/{id}/property")
    @Operation(summary = "Lidh lead-in me një pronë (ADMIN/AGENT)")
    public ResponseEntity<LeadResponse> linkProperty(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return ok(leadService.linkProperty(id, body.get("property_id")));
    }
}