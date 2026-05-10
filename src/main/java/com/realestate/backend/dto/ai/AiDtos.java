package com.realestate.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AiDtos {

    // ── 1. Property Description ───────────────────────────────────
    public record PropertyDescriptionRequest(
            String type, Integer bedrooms, Integer bathrooms,
            String areaSqm, Integer floor, Integer yearBuilt,
            String city, String features, String price
    ) {}

    public record PropertyDescriptionResponse(
            String title, String description
    ) {}

    // ── 2. Price Estimator ────────────────────────────────────────
    public record PriceEstimateRequest(
            String type, String areaSqm, Integer bedrooms,
            String city, Integer floor, Integer totalFloors,
            Integer yearBuilt, String listingType
    ) {}

    public record PriceEstimateResponse(
            @JsonProperty("estimated_price") double estimatedPrice,
            @JsonProperty("price_per_sqm")   double pricePerSqm,
            String confidence, String reasoning
    ) {}

    // ── 3. Chat ───────────────────────────────────────────────────
    public record ChatMessage(String role, String content) {}

    public record ChatRequest(
            String message,
            List<ChatMessage> history
    ) {}

    public record ChatResponse(String message, String role) {}

    // ── 4. Contract Summary ───────────────────────────────────────
    public record ContractSummaryRequest(
            @JsonProperty("contract_id") Long contractId,
            @JsonProperty("property_id") Long propertyId,
            @JsonProperty("client_id")   Long clientId,
            @JsonProperty("agent_id")    Long agentId,
            @JsonProperty("start_date")  String startDate,
            @JsonProperty("end_date")    String endDate,
            String rent, String deposit, String status
    ) {}

    public record ContractSummaryResponse(
            String summary,
            @JsonProperty("key_dates")              String keyDates,
            @JsonProperty("financial_obligations")  String financialObligations,
            String risks,
            @JsonProperty("status_note")            String statusNote
    ) {}

    // ── 5. Payment Risk ───────────────────────────────────────────
    public record PaymentRiskResponse(
            @JsonProperty("client_id")        Long clientId,
            @JsonProperty("risk_score")        int riskScore,
            @JsonProperty("risk_level")        String riskLevel,
            String reasoning, String recommendation,
            @JsonProperty("total_payments")    int totalPayments,
            @JsonProperty("overdue_payments")  int overduePayments
    ) {}

    // ── 6. Lead Match ─────────────────────────────────────────────
    public record LeadMatchRequest(
            @JsonProperty("budget_min") String budgetMin,
            @JsonProperty("budget_max") String budgetMax,
            String type, String city,
            Integer bedrooms,
            @JsonProperty("lead_type") String leadType
    ) {}

    public record LeadMatchResponse(
            @JsonProperty("raw_response") String rawResponse,
            String summary
    ) {}

    // ── 7. Agent Performance ──────────────────────────────────
    public record AgentPerformanceRequest(
            @JsonProperty("agent_id")      Long   agentId,
            @JsonProperty("agent_name")    String agentName,
            @JsonProperty("total_leads")   int    totalLeads,
            @JsonProperty("done_leads")    int    doneLeads,
            @JsonProperty("active_leases") int    activeLeases,
            @JsonProperty("total_sales")   int    totalSales,
            @JsonProperty("revenue")       String revenue
    ) {}

    public record AgentPerformanceResponse(
            @JsonProperty("agent_id")      Long   agentId,
            @JsonProperty("score")         int    score,
            @JsonProperty("level")         String level,
            @JsonProperty("strengths")     String strengths,
            @JsonProperty("weaknesses")    String weaknesses,
            @JsonProperty("recommendation") String recommendation
    ) {}
}