package com.realestate.backend.controller;

import com.realestate.backend.dto.ai.AiDtos.*;
import com.realestate.backend.entity.enums.LeadStatus;
import com.realestate.backend.repository.*;
import com.realestate.backend.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
@Tag(name = "Admin AI Analytics")
@SecurityRequirement(name = "BearerAuth")
public class AdminAiController extends BaseController {

    private final AiService              aiService;
    private final UserRepository         userRepo;
    private final LeadRequestRepository  leadRepo;
    private final LeaseContractRepository contractRepo;
    private final SaleContractRepository saleContractRepo;
    private final PaymentRepository      paymentRepo;

    @GetMapping("/agent/{agentId}/performance")
    @Operation(summary = "Analizë AI e performancës së agjentit (vetëm ADMIN)")
    public ResponseEntity<AgentPerformanceResponse> analyzeAgent(
            @PathVariable Long agentId) {

        String agentName = userRepo.findFullNameById(agentId)
                .orElse("Agent #" + agentId);

        long totalLeads = leadRepo.countByAssignedAgentIdAndStatus(
                agentId, LeadStatus.DONE)
                + leadRepo.countByAssignedAgentIdAndStatus(
                agentId, LeadStatus.IN_PROGRESS)
                + leadRepo.countByAssignedAgentIdAndStatus(
                agentId, LeadStatus.NEW);

        long doneLeads = leadRepo.countByAssignedAgentIdAndStatus(
                agentId, LeadStatus.DONE);

        long activeLeases = contractRepo.findByAgentIdOrderByCreatedAtDesc(
                        agentId,
                        org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();

        long totalSales = saleContractRepo
                .findByAgentIdOrderByCreatedAtDesc(
                        agentId,
                        org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();

        BigDecimal revenue = paymentRepo.totalRevenue();

        AgentPerformanceRequest req = new AgentPerformanceRequest(
                agentId, agentName,
                (int) totalLeads, (int) doneLeads,
                (int) activeLeases, (int) totalSales,
                revenue != null ? revenue.toPlainString() : "0"
        );

        return ok(aiService.analyzeAgentPerformance(req));
    }
}