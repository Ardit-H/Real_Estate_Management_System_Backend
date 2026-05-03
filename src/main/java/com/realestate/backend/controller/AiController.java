package com.realestate.backend.controller;

import com.realestate.backend.dto.ai.AiDtos.*;
import com.realestate.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features")
@SecurityRequirement(name = "BearerAuth")
public class AiController {

    private final AiService aiService;
    @Value("${gemini.api.key:placeholder}")
    private String apiKey;
    // ── 1. Property Description Generator ────────────────────────
    @PostMapping("/property/description")
    @Operation(summary = "Generate property title and description using AI")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PropertyDescriptionResponse> generateDescription(
            @RequestBody PropertyDescriptionRequest request) {
        return ResponseEntity.ok(aiService.generateDescription(request));
    }

    // ── 2. Price Estimator ────────────────────────────────────────
    @PostMapping("/property/estimate")
    @Operation(summary = "Estimate property price using AI based on market data")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PriceEstimateResponse> estimatePrice(
            @RequestBody PriceEstimateRequest request) {
        return ResponseEntity.ok(aiService.estimatePrice(request));
    }

    // ── 3. Chat Assistant ─────────────────────────────────────────
    @PostMapping("/chat")
    @Operation(summary = "AI chat assistant for clients — property search help")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT','CLIENT')")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiService.chat(request));
    }

    // ── 4. Contract Summarizer ────────────────────────────────────
    @PostMapping("/contract/summary")
    @Operation(summary = "Summarize a lease contract in plain language")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<ContractSummaryResponse> summarizeContract(
            @RequestBody ContractSummaryRequest request) {
        return ResponseEntity.ok(aiService.summarizeContract(request));
    }

    // ── 5. Payment Risk Detector ──────────────────────────────────
    @GetMapping("/payments/risk/{clientId}")
    @Operation(summary = "Analyze payment risk score for a client")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public ResponseEntity<PaymentRiskResponse> detectRisk(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(aiService.detectPaymentRisk(clientId));
    }

}