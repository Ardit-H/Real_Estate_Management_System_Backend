package com.realestate.backend.service;

import com.realestate.backend.dto.ai.AiDtos.*;
import com.realestate.backend.entity.enums.PaymentStatus;
import com.realestate.backend.entity.enums.PropertyStatus;
import com.realestate.backend.entity.rental.LeaseContract;
import com.realestate.backend.entity.rental.Payment;
import com.realestate.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock private PropertyRepository      propertyRepo;
    @Mock private PaymentRepository       paymentRepo;
    @Mock private LeaseContractRepository contractRepo;

    @InjectMocks
    private AiService aiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(aiService, "apiKey", "placeholder");
    }

    // ══════════════════════════════════════════════════════════════
    // 1. generateDescription
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("generateDescription - kthen title dhe description nga mock")
    void generateDescription_returnsMockResponse() {
        // String type, Integer bedrooms, Integer bathrooms,
        // String areaSqm, Integer floor, Integer yearBuilt,
        // String city, String features, String price
        PropertyDescriptionRequest req = new PropertyDescriptionRequest(
                "APARTMENT", 2, 1,
                "85", 3, 2015,
                "Prishtinë", "parking, balcony", "95000"
        );

        PropertyDescriptionResponse resp = aiService.generateDescription(req);

        assertThat(resp).isNotNull();
        assertThat(resp.title()).isNotBlank();
        assertThat(resp.description()).isNotBlank();
    }

    @Test
    @DisplayName("generateDescription - title dhe description nuk janë null")
    void generateDescription_titleAndDescriptionNotNull() {
        PropertyDescriptionRequest req = new PropertyDescriptionRequest(
                "VILLA", 4, 2,
                "200", 1, 2020,
                "Tiranë", "pool, garden", "250000"
        );

        PropertyDescriptionResponse resp = aiService.generateDescription(req);

        assertThat(resp.title()).isNotNull();
        assertThat(resp.description()).isNotNull();
    }

    @Test
    @DisplayName("generateDescription - nuk thirret asnjë repository")
    void generateDescription_doesNotCallRepositories() {
        PropertyDescriptionRequest req = new PropertyDescriptionRequest(
                "HOUSE", 3, 2,
                "120", 2, 2018,
                "Gjilan", "parking", "150000"
        );

        aiService.generateDescription(req);

        verifyNoInteractions(propertyRepo, paymentRepo, contractRepo);
    }

    // ══════════════════════════════════════════════════════════════
    // 2. estimatePrice
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("estimatePrice - kthen çmim pozitiv dhe confidence")
    void estimatePrice_returnsPositiveValues() {
        when(propertyRepo.countByStatus(PropertyStatus.AVAILABLE)).thenReturn(42L);

        // String type, String areaSqm, Integer bedrooms,
        // String city, Integer floor, Integer totalFloors,
        // Integer yearBuilt, String listingType
        PriceEstimateRequest req = new PriceEstimateRequest(
                "APARTMENT", "85", 2,
                "Prishtinë", 3, 5,
                2015, "SALE"
        );

        PriceEstimateResponse resp = aiService.estimatePrice(req);

        assertThat(resp).isNotNull();
        assertThat(resp.estimatedPrice()).isGreaterThanOrEqualTo(0);
        assertThat(resp.pricePerSqm()).isGreaterThanOrEqualTo(0);
        assertThat(resp.confidence()).isNotBlank();
        assertThat(resp.reasoning()).isNotBlank();
    }

    @Test
    @DisplayName("estimatePrice - thirr propertyRepo.countByStatus(AVAILABLE)")
    void estimatePrice_callsPropertyRepo() {
        when(propertyRepo.countByStatus(PropertyStatus.AVAILABLE)).thenReturn(10L);

        PriceEstimateRequest req = new PriceEstimateRequest(
                "HOUSE", "120", 3,
                "Gjilan", 1, 2,
                2018, "SALE"
        );

        aiService.estimatePrice(req);

        verify(propertyRepo, times(1)).countByStatus(PropertyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("estimatePrice - nuk thirret paymentRepo apo contractRepo")
    void estimatePrice_doesNotCallOtherRepos() {
        when(propertyRepo.countByStatus(PropertyStatus.AVAILABLE)).thenReturn(5L);

        PriceEstimateRequest req = new PriceEstimateRequest(
                "COMMERCIAL", "300", 0,
                "Ferizaj", 0, 1,
                2019, "RENT"
        );

        aiService.estimatePrice(req);

        verifyNoInteractions(paymentRepo, contractRepo);
    }

    // ══════════════════════════════════════════════════════════════
    // 3. chat — mock mode (placeholder key)
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("chat - mesazh për blerje kthen udhëzim për sale listings")
    void chat_buyKeyword_returnsSaleListingsAdvice() {
        ChatRequest req = new ChatRequest("I want to buy a house", null);

        ChatResponse resp = aiService.chat(req);

        assertThat(resp).isNotNull();
        assertThat(resp.message()).isNotBlank();
        assertThat(resp.role()).isEqualTo("assistant");
        assertThat(resp.message()).containsIgnoringCase("sale listings");
    }

    @Test
    @DisplayName("chat - mesazh për qira kthen udhëzim për rentals")
    void chat_rentKeyword_returnsRentalAdvice() {
        ChatRequest req = new ChatRequest("rent apartment", null);

        ChatResponse resp = aiService.chat(req);

        assertThat(resp.message()).containsIgnoringCase("rental");
    }

    @Test
    @DisplayName("chat - mesazh për çmim kthen udhëzim budget")
    void chat_priceKeyword_returnsBudgetAdvice() {
        ChatRequest req = new ChatRequest("affordable cheap budget", null);

        ChatResponse resp = aiService.chat(req);

        assertThat(resp.message()).containsIgnoringCase("budget");
    }

    @Test
    @DisplayName("chat - mesazh i panjohur kthen default response")
    void chat_unknownMessage_returnsDefaultResponse() {
        ChatRequest req = new ChatRequest("xyzabc123", null);

        ChatResponse resp = aiService.chat(req);

        assertThat(resp.message()).isNotBlank();
        assertThat(resp.role()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("chat - mesazh përshëndetje kthen welcome")
    void chat_greetingMessage_returnsWelcome() {
        ChatRequest req = new ChatRequest("hello", null);

        ChatResponse resp = aiService.chat(req);

        assertThat(resp.message()).containsIgnoringCase("welcome");
    }

    @Test
    @DisplayName("chat - nuk thirret asnjë repository")
    void chat_doesNotCallAnyRepository() {
        ChatRequest req = new ChatRequest("looking for a property", null);

        aiService.chat(req);

        verifyNoInteractions(propertyRepo, paymentRepo, contractRepo);
    }

    @Test
    @DisplayName("chat - me historik → nuk hedh exception")
    void chat_withHistory_doesNotThrow() {
        List<ChatMessage> history = List.of(
                new ChatMessage("user",      "I need help"),
                new ChatMessage("assistant", "How can I assist?")
        );
        ChatRequest req = new ChatRequest("find me a house", history);

        assertThatCode(() -> aiService.chat(req)).doesNotThrowAnyException();
    }

    // ══════════════════════════════════════════════════════════════
    // 4. summarizeContract
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("summarizeContract - kthen të gjitha fushat e nevojshme")
    void summarizeContract_returnsAllFields() {
        // Long contractId, Long propertyId, Long clientId, Long agentId,
        // String startDate, String endDate,
        // String rent, String deposit, String status
        ContractSummaryRequest req = new ContractSummaryRequest(
                1L, 10L, 5L, 3L,
                "2024-01-01", "2025-01-01",
                "500", "1000", "ACTIVE"
        );

        ContractSummaryResponse resp = aiService.summarizeContract(req);

        assertThat(resp).isNotNull();
        assertThat(resp.summary()).isNotBlank();
        assertThat(resp.keyDates()).isNotBlank();
        assertThat(resp.financialObligations()).isNotBlank();
        assertThat(resp.risks()).isNotBlank();
        assertThat(resp.statusNote()).isNotBlank();
    }

    @Test
    @DisplayName("summarizeContract - nuk thirret asnjë repository")
    void summarizeContract_doesNotCallRepositories() {
        ContractSummaryRequest req = new ContractSummaryRequest(
                2L, 20L, 8L, 4L,
                "2024-06-01", "2025-06-01",
                "700", "1400", "ACTIVE"
        );

        aiService.summarizeContract(req);

        verifyNoInteractions(propertyRepo, paymentRepo, contractRepo);
    }

    // ══════════════════════════════════════════════════════════════
    // 5. detectPaymentRisk
    // ══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("detectPaymentRisk - klient pa kontrata kthen response të vlefshme")
    void detectPaymentRisk_noContracts_returnsValidResponse() {
        Long clientId = 1L;
        when(contractRepo.findActiveByClient(clientId)).thenReturn(List.of());

        PaymentRiskResponse resp = aiService.detectPaymentRisk(clientId);

        assertThat(resp).isNotNull();
        assertThat(resp.clientId()).isEqualTo(clientId);
        assertThat(resp.riskScore()).isGreaterThanOrEqualTo(1);
        assertThat(resp.riskLevel()).isNotBlank();
        assertThat(resp.totalPayments()).isEqualTo(0);
        assertThat(resp.overduePayments()).isEqualTo(0);
    }

    @Test
    @DisplayName("detectPaymentRisk - me pagesa OVERDUE thirr paymentRepo saktë")
    void detectPaymentRisk_withOverduePayments_callsPaymentRepo() {
        Long clientId = 2L;

        LeaseContract contract = new LeaseContract();
        contract.setId(10L);

        Payment overduePayment = new Payment();
        overduePayment.setStatus(PaymentStatus.OVERDUE);

        Payment paidPayment = new Payment();
        paidPayment.setStatus(PaymentStatus.PAID);

        when(contractRepo.findActiveByClient(clientId)).thenReturn(List.of(contract));
        when(paymentRepo.findByContract_IdOrderByDueDateAsc(10L))
                .thenReturn(List.of(overduePayment, paidPayment));

        PaymentRiskResponse resp = aiService.detectPaymentRisk(clientId);

        assertThat(resp.clientId()).isEqualTo(clientId);
        assertThat(resp.totalPayments()).isEqualTo(2);
        assertThat(resp.overduePayments()).isEqualTo(1);
        verify(paymentRepo).findByContract_IdOrderByDueDateAsc(10L);
    }

    @Test
    @DisplayName("detectPaymentRisk - të gjitha pagesat PAID → overdue = 0")
    void detectPaymentRisk_allPaid_overdueIsZero() {
        Long clientId = 3L;

        LeaseContract contract = new LeaseContract();
        contract.setId(20L);

        Payment p1 = new Payment(); p1.setStatus(PaymentStatus.PAID);
        Payment p2 = new Payment(); p2.setStatus(PaymentStatus.PAID);
        Payment p3 = new Payment(); p3.setStatus(PaymentStatus.PAID);

        when(contractRepo.findActiveByClient(clientId)).thenReturn(List.of(contract));
        when(paymentRepo.findByContract_IdOrderByDueDateAsc(20L))
                .thenReturn(List.of(p1, p2, p3));

        PaymentRiskResponse resp = aiService.detectPaymentRisk(clientId);

        assertThat(resp.overduePayments()).isEqualTo(0);
        assertThat(resp.totalPayments()).isEqualTo(3);
    }

    @Test
    @DisplayName("detectPaymentRisk - thirr contractRepo.findActiveByClient me ID të saktë")
    void detectPaymentRisk_callsContractRepoWithCorrectId() {
        Long clientId = 99L;
        when(contractRepo.findActiveByClient(clientId)).thenReturn(List.of());

        aiService.detectPaymentRisk(clientId);

        verify(contractRepo).findActiveByClient(99L);
    }
}
