package com.realestate.backend.controller;

import com.realestate.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController extends BaseController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Stats të përgjithshme për dashboard (cached 10 min)")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ok(dashboardService.getStats());
    }
}