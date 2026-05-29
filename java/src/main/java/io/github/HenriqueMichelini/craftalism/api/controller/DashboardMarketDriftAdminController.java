package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketDriftResetResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.service.MarketDriftAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/market/drift")
public class DashboardMarketDriftAdminController {

    private final MarketDriftAdminService adminService;

    public DashboardMarketDriftAdminController(
        MarketDriftAdminService adminService
    ) {
        this.adminService = adminService;
    }

    @PostMapping("/reset")
    public ResponseEntity<MarketDriftResetResponseDTO> resetDrift() {
        return ResponseEntity.ok(adminService.resetAllDrift());
    }
}
