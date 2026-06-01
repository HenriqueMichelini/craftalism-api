package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminCancelRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventAdminUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventAdminService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/market/events")
public class DashboardMarketEventAdminController {

    private final MarketEventAdminService adminService;

    public DashboardMarketEventAdminController(
        MarketEventAdminService adminService
    ) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<List<MarketEventAdminResponseDTO>> listEvents() {
        return ResponseEntity.ok(adminService.listEvents());
    }

    @PostMapping
    public ResponseEntity<MarketEventAdminResponseDTO> startEvent(
        JwtAuthenticationToken authentication,
        @RequestBody @Valid MarketEventAdminCreateRequestDTO request
    ) {
        MarketEventAdminResponseDTO event = adminService.startEvent(
            request,
            actor(authentication)
        );
        return ResponseEntity.created(
            URI.create("/api/dashboard/market/events/" + event.id())
        ).body(event);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MarketEventAdminResponseDTO> updateEvent(
        JwtAuthenticationToken authentication,
        @PathVariable Long id,
        @RequestBody @Valid MarketEventAdminUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
            adminService.updateEvent(id, request, actor(authentication))
        );
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<MarketEventAdminResponseDTO> cancelEvent(
        JwtAuthenticationToken authentication,
        @PathVariable Long id,
        @RequestBody MarketEventAdminCancelRequestDTO request
    ) {
        return ResponseEntity.ok(
            adminService.cancelEvent(id, request, actor(authentication))
        );
    }

    @PostMapping("/supersede")
    public ResponseEntity<MarketEventAdminResponseDTO> supersedeEvent(
        JwtAuthenticationToken authentication,
        @RequestBody @Valid MarketEventAdminCreateRequestDTO request
    ) {
        return ResponseEntity.ok(
            adminService.supersedeActiveEvent(request, actor(authentication))
        );
    }

    private String actor(JwtAuthenticationToken authentication) {
        return authentication == null ? "unknown" : authentication.getName();
    }
}
