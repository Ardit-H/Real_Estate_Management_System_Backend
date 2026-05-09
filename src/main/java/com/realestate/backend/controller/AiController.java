package com.realestate.backend.controller;

import com.realestate.backend.dto.ai.AiDtos.*;
import com.realestate.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features")
@SecurityRequirement(name = "BearerAuth")
public class AiController extends BaseController {

    private final AiService aiService;

    @PostMapping("/property/description")
    @Operation(summary = "Generate property title and description using AI")
    public ResponseEntity<PropertyDescriptionResponse> generateDescription(
            @RequestBody PropertyDescriptionRequest request) {
        return ok(aiService.generateDescription(request));
    }

    @PostMapping("/property/estimate")
    @Operation(summary = "Estimate property price using AI based on market data")
    public ResponseEntity<PriceEstimateResponse> estimatePrice(
            @RequestBody PriceEstimateRequest request) {
        return ok(aiService.estimatePrice(request));
    }

    @PostMapping("/chat")
    @Operation(summary = "AI chat assistant for clients — property search help")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request) {
        return ok(aiService.chat(request));
    }

    @PostMapping("/contract/summary")
    @Operation(summary = "Summarize a lease contract in plain language")
    public ResponseEntity<ContractSummaryResponse> summarizeContract(
            @RequestBody ContractSummaryRequest request) {
        return ok(aiService.summarizeContract(request));
    }

    @GetMapping("/payments/risk/{clientId}")
    @Operation(summary = "Analyze payment risk score for a client")
    public ResponseEntity<PaymentRiskResponse> detectRisk(
            @PathVariable Long clientId) {
        return ok(aiService.detectPaymentRisk(clientId));
    }
}