package com.realestate.backend.service;

import com.realestate.backend.dto.ai.AiDtos.*;
import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.enums.PropertyType;
import com.realestate.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final PropertyRepository      propertyRepo;
    private final PaymentRepository       paymentRepo;
    private final LeaseContractRepository contractRepo;

    @Value("${groq.api.key:placeholder}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    private final RestTemplate restTemplate = new RestTemplate();

    // ── 1. Property Description Generator ────────────────────────
    public PropertyDescriptionResponse generateDescription(PropertyDescriptionRequest req) {
        String prompt = String.format(
                "You are a professional real estate copywriter.\n" +
                        "Generate a compelling property listing in English.\n" +
                        "Property: Type=%s, Bedrooms=%s, Bathrooms=%s, Area=%s sqm, " +
                        "Floor=%s, Year=%s, City=%s, Features=%s, Price=%s EUR\n" +
                        "Return ONLY this JSON (no other text, no markdown):\n" +
                        "{\"title\": \"short catchy title max 10 words\", \"description\": \"3-4 sentence description\"}",
                req.type(), req.bedrooms(), req.bathrooms(), req.areaSqm(),
                req.floor(), req.yearBuilt(), req.city(), req.features(), req.price()
        );

        String raw = callOpenAI(prompt);
        log.info("OpenAI description response: {}", raw);
        try {
            return new PropertyDescriptionResponse(
                    extractField(raw, "title"),
                    extractField(raw, "description")
            );
        } catch (Exception e) {
            log.warn("AI description parse failed: {}", e.getMessage());
            return new PropertyDescriptionResponse("Property Listing", raw);
        }
    }

    // ── 2. Price Estimator ────────────────────────────────────────
    public PriceEstimateResponse estimatePrice(PriceEstimateRequest req) {
        long availableCount = propertyRepo.countByStatus(PropertyStatus.AVAILABLE);

        String prompt = String.format(
                "You are a real estate pricing expert in Kosovo/Albania/Balkans.\n" +
                        "Estimate fair market price for: Type=%s, Area=%s sqm, Bedrooms=%s, " +
                        "City=%s, Floor=%s/%s, Year=%s, ListingType=%s\n" +
                        "Market context: %d available properties in system.\n" +
                        "Return ONLY this JSON (no markdown):\n" +
                        "{\"estimated_price\": 85000, \"price_per_sqm\": 850, " +
                        "\"confidence\": \"HIGH\", \"reasoning\": \"brief explanation\"}",
                req.type(), req.areaSqm(), req.bedrooms(), req.city(),
                req.floor(), req.totalFloors(), req.yearBuilt(),
                req.listingType(), availableCount
        );

        String raw = callOpenAI(prompt);
        log.info("OpenAI price response: {}", raw);
        try {
            double price  = parseDouble(extractField(raw, "estimated_price"));
            double perSqm = parseDouble(extractField(raw, "price_per_sqm"));
            return new PriceEstimateResponse(price, perSqm,
                    extractField(raw, "confidence"),
                    extractField(raw, "reasoning")
            );
        } catch (Exception e) {
            log.warn("AI price parse failed: {}", e.getMessage());
            return new PriceEstimateResponse(0, 0, "LOW", raw);
        }
    }

    // ── 3. Client Chat Assistant ──────────────────────────────────
    public ChatResponse chat(ChatRequest req) {
        // Nëse nuk ka API key → mock merr VETËM mesazhin e userit
        if (apiKey.equals("placeholder") || apiKey.isBlank()) {
            String reply = getChatMock(req.message().toLowerCase().trim());
            return new ChatResponse(reply, "assistant");
        }

        // OpenAI — messages array me historik
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are a helpful real estate assistant for a property management system " +
                        "in Kosovo/Albania/Balkans. Help clients find properties, understand contracts, " +
                        "and answer questions. Be concise, friendly, professional. " +
                        "Always answer in English only, regardless of the language the user writes in. Max 150 words per response."
        ));

        if (req.history() != null) {
            for (ChatMessage msg : req.history()) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", req.message()));

        String reply = callOpenAIWithMessages(messages);
        return new ChatResponse(reply.trim(), "assistant");
    }

    // ── 4. Contract Summarizer ────────────────────────────────────
    public ContractSummaryResponse summarizeContract(ContractSummaryRequest req) {
        String prompt = String.format(
                "You are a legal assistant for real estate contracts.\n" +
                        "Summarize this lease in simple language:\n" +
                        "Contract #%d | Property #%s | Client #%s | Agent #%s\n" +
                        "Period: %s to %s | Rent: %s EUR/month | Deposit: %s EUR | Status: %s\n" +
                        "Return ONLY this JSON (no markdown):\n" +
                        "{\"summary\": \"2-sentence summary\", " +
                        "\"key_dates\": \"important dates\", " +
                        "\"financial_obligations\": \"what client owes\", " +
                        "\"risks\": \"potential risks\", " +
                        "\"status_note\": \"what status means\"}",
                req.contractId(), req.propertyId(), req.clientId(), req.agentId(),
                req.startDate(), req.endDate(), req.rent(), req.deposit(), req.status()
        );

        String raw = callOpenAI(prompt);
        try {
            return new ContractSummaryResponse(
                    extractField(raw, "summary"),
                    extractField(raw, "key_dates"),
                    extractField(raw, "financial_obligations"),
                    extractField(raw, "risks"),
                    extractField(raw, "status_note")
            );
        } catch (Exception e) {
            log.warn("AI contract parse failed: {}", e.getMessage());
            return new ContractSummaryResponse(raw, "—", "—", "—", "—");
        }
    }

    // ── 5. Payment Risk Detector ──────────────────────────────────
    public PaymentRiskResponse detectPaymentRisk(Long clientId) {
        var contracts = contractRepo.findActiveByClient(clientId);

        long total   = 0;
        long overdue = 0;
        for (var c : contracts) {
            var payments = paymentRepo.findByContract_IdOrderByDueDateAsc(c.getId());
            total   += payments.size();
            overdue += payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.OVERDUE).count();
        }

        String onTimeRate = total > 0
                ? String.format("%.0f", ((double)(total - overdue) / total) * 100)
                : "N/A";

        String prompt = String.format(
                "You are a financial risk analyst for a real estate company.\n" +
                        "Analyze payment behavior:\n" +
                        "Active contracts: %d | Total payments: %d | " +
                        "Overdue payments: %d | On-time rate: %s%%\n" +
                        "Return ONLY this JSON (no markdown):\n" +
                        "{\"risk_score\": 3, \"risk_level\": \"LOW\", " +
                        "\"reasoning\": \"explanation\", \"recommendation\": \"action to take\"}\n" +
                        "Score: 1-3=LOW, 4-6=MEDIUM, 7-8=HIGH, 9-10=CRITICAL",
                contracts.size(), total, overdue, onTimeRate
        );

        String raw = callOpenAI(prompt);
        try {
            int score = (int) parseDouble(extractField(raw, "risk_score"));
            return new PaymentRiskResponse(
                    clientId, score,
                    extractField(raw, "risk_level"),
                    extractField(raw, "reasoning"),
                    extractField(raw, "recommendation"),
                    (int) total, (int) overdue
            );
        } catch (Exception e) {
            log.warn("AI risk parse failed: {}", e.getMessage());
            return new PaymentRiskResponse(clientId, 5, "MEDIUM", raw, "Review manually", (int) total, (int) overdue);
        }
    }

    public AgentPerformanceResponse analyzeAgentPerformance(AgentPerformanceRequest req) {
        String prompt = String.format(
                "You are an HR analyst for a real estate company.\n" +
                        "Analyze the performance of agent '%s' (id=%d):\n" +
                        "- Total leads assigned: %d\n" +
                        "- Leads completed (DONE): %d\n" +
                        "- Active lease contracts managed: %d\n" +
                        "- Total sales closed: %d\n" +
                        "- Revenue generated: %s EUR\n" +
                        "Return ONLY this JSON (no markdown):\n" +
                        "{\"score\": 7, \"level\": \"GOOD\", " +
                        "\"strengths\": \"what they do well\", " +
                        "\"weaknesses\": \"what needs improvement\", " +
                        "\"recommendation\": \"concrete action\"}",
                req.agentName(), req.agentId(),
                req.totalLeads(), req.doneLeads(),
                req.activeLeases(), req.totalSales(), req.revenue()
        );

        String raw = callOpenAI(prompt);
        try {
            int score = (int) parseDouble(extractField(raw, "score"));
            String level = score >= 8 ? "EXCELLENT"
                    : score >= 6 ? "GOOD"
                    : score >= 4 ? "AVERAGE" : "NEEDS_IMPROVEMENT";
            return new AgentPerformanceResponse(
                    req.agentId(), score, level,
                    extractField(raw, "strengths"),
                    extractField(raw, "weaknesses"),
                    extractField(raw, "recommendation")
            );
        } catch (Exception e) {
            log.warn("AI agent performance parse failed: {}", e.getMessage());
            return new AgentPerformanceResponse(
                    req.agentId(), 5, "AVERAGE", raw, "N/A", "Review manually");
        }
    }

    // ════════════════════════════════════════════════════════════
    // OPENAI API CALLS
    // ════════════════════════════════════════════════════════════

    private String callOpenAI(String prompt) {
        return callOpenAIWithMessages(
                List.of(Map.of("role", "user", "content", prompt))
        );
    }

    private String callOpenAIWithMessages(List<Map<String, String>> messages) {
        try {
            if (apiKey.equals("placeholder") || apiKey.isBlank()) {
                String lastMsg = messages.get(messages.size() - 1).get("content");
                return getMockResponseForFeature(lastMsg);
            }

            log.info("Calling OpenAI with key: {}...", apiKey.substring(0, Math.min(10, apiKey.length())));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("max_tokens", 500);
            body.put("temperature", 0.7);

            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    OPENAI_URL,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (resp.getBody() != null) {
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) resp.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message =
                            (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "AI service unavailable";

        } catch (Exception e) {
            log.error("OpenAI API error: {}", e.getMessage());
            String lastMsg = messages.get(messages.size() - 1).get("content");
            return getMockResponseForFeature(lastMsg);
        }
    }

    // ════════════════════════════════════════════════════════════
    // MOCK — për features (description, price, risk, etj.)
    // ════════════════════════════════════════════════════════════

    private String getMockResponseForFeature(String prompt) {
        String p = prompt.toLowerCase();
        if (p.contains("copywriter"))
            return "{\"title\": \"Modern Property in Prime Location\", \"description\": \"A beautiful well-maintained property featuring modern amenities and excellent location. Perfect for families or professionals seeking quality living.\"}";
        if (p.contains("pricing expert"))
            return "{\"estimated_price\": 85000, \"price_per_sqm\": 850, \"confidence\": \"MEDIUM\", \"reasoning\": \"Based on location and property specifications in the local market.\"}";
        if (p.contains("risk_score"))
            return "{\"risk_score\": 3, \"risk_level\": \"LOW\", \"reasoning\": \"Client has a clean payment record.\", \"recommendation\": \"Proceed normally.\"}";
        if (p.contains("match_score"))
            return "{\"matches\": [{\"property_id\": 1, \"match_score\": 85, \"reason\": \"Matches client budget and location\"}], \"summary\": \"Found 1 suitable property.\"}";
        if (p.contains("status_note"))
            return "{\"summary\": \"Standard lease agreement.\", \"key_dates\": \"Review start and end dates.\", \"financial_obligations\": \"Monthly rent due.\", \"risks\": \"Deposit refund conditions apply.\", \"status_note\": \"Contract is active.\"}";
        return "AI mock response";
    }



    private String getChatMock(String userMessage) {
        if (userMessage.contains("cmim") || userMessage.contains("çmim") ||
                userMessage.contains("lir") || userMessage.contains("cheap") ||
                userMessage.contains("afford") || userMessage.contains("budget"))
            return "To find the most affordable properties, use the price filter in Browse Properties and set your maximum budget. Do you have a specific budget in mind?";

        if (userMessage.contains("mitrovic") || userMessage.contains("prizren") ||
                userMessage.contains("prishtin") || userMessage.contains("gjakov") ||
                userMessage.contains("ferizaj") || userMessage.contains("pej") ||
                userMessage.contains("qytet") || userMessage.contains("city"))
            return "Go to Browse Properties and filter by city to see all available listings there. Which city are you interested in?";

        if (userMessage.contains("qira") || userMessage.contains("rent") ||
                userMessage.contains("me qira"))
            return "For rentals, check out the Rental Listings section. You can apply directly for any listing that interests you. Which city and what budget do you have?";

        if (userMessage.contains("blerj") || userMessage.contains("bli") ||
                userMessage.contains("buy") || userMessage.contains("shites") ||
                userMessage.contains("shitj"))
            return "For purchasing a property, check the Sale Listings section. You can submit an offer directly from the listing page. What is your budget and preferred city?";

        if (userMessage.contains("dhom") || userMessage.contains("room") ||
                userMessage.contains("bed"))
            return "You can filter properties by number of bedrooms in Browse Properties. How many bedrooms do you need?";

        if (userMessage.contains("kontrat") || userMessage.contains("contract") ||
                userMessage.contains("dokument"))
            return "Contracts are signed after your application is approved. The agent will contact you with the details. Do you have a specific question about the contract?";

        if (userMessage.contains("agjent") || userMessage.contains("agent") ||
                userMessage.contains("kontakt") || userMessage.contains("contact"))
            return "You can contact the agent by clicking directly on the property listing you are interested in. Agents are available during business hours.";

        if (userMessage.contains("apartament") || userMessage.contains("apart") ||
                userMessage.contains("flat"))
            return "We have many apartments available! Go to Browse Properties, select Type = APARTMENT and filter by city and price. Are you looking to rent or buy?";

        if (userMessage.contains("vil") || userMessage.contains("shtëpi") ||
                userMessage.contains("house"))
            return "For villas and houses, go to Browse Properties and select Type = HOUSE or VILLA. Do you have a preferred city or budget?";

        if (userMessage.contains("gjej") || userMessage.contains("kërko") ||
                userMessage.contains("search") || userMessage.contains("find") ||
                userMessage.contains("dua") || userMessage.contains("looking"))
            return "Happy to help! To find the right property, use Browse Properties where you can filter by city, price, bedrooms and type. What exactly are you looking for?";

        if (userMessage.contains("faleminderit") || userMessage.contains("thank") ||
                userMessage.contains("mirupafshim") || userMessage.contains("bye"))
            return "Thank you! If you need any more help, I am here. I hope you find your dream property! 🏠";

        if (userMessage.contains("hello") || userMessage.contains("hi") ||
                userMessage.contains("hey") || userMessage.contains("tungjatjeta") ||
                userMessage.contains("mirëdita") || userMessage.contains("miredit") ||
                userMessage.contains("pershendetje"))
            return "Welcome! I am your real estate assistant. I can help you find properties by budget, city or type. How can I help you?";

        if (userMessage.contains("po") || userMessage.contains("yes") ||
                userMessage.contains("ok") || userMessage.contains("shumë mirë") ||
                userMessage.length() < 5)
            return "Great! To view available listings go to Browse Properties. Are you looking to rent or buy?";

        return "I understand your question! To find the property you are looking for, use Browse Properties where you can filter by city, price, bedrooms and type. Can you tell me more about what you are looking for?";
    }

    // ════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════

    private String extractField(String json, String field) {
        json = json.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

        String search = "\"" + field + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int colon = json.indexOf(":", idx + search.length());
        if (colon == -1) return "";

        String rest = json.substring(colon + 1).trim();

        if (rest.startsWith("\"")) {
            StringBuilder sb = new StringBuilder();
            int i = 1;
            while (i < rest.length()) {
                char c = rest.charAt(i);
                if (c == '\\' && i + 1 < rest.length()) {
                    sb.append(rest.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (c == '"') break;
                sb.append(c);
                i++;
            }
            return sb.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == ',' || c == '}' || c == '\n') break;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private double parseDouble(String s) {
        return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
    }
}