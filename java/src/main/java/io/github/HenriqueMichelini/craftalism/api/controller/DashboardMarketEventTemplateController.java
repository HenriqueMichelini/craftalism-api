package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketEventTemplateUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/market/event-templates")
public class DashboardMarketEventTemplateController {

    private final MarketEventTemplateService templateService;

    public DashboardMarketEventTemplateController(
        MarketEventTemplateService templateService
    ) {
        this.templateService = templateService;
    }

    @GetMapping
    public ResponseEntity<List<MarketEventTemplateResponseDTO>> listTemplates() {
        return ResponseEntity.ok(templateService.listTemplates());
    }

    @PostMapping
    public ResponseEntity<MarketEventTemplateResponseDTO> createTemplate(
        @RequestBody @Valid MarketEventTemplateCreateRequestDTO request
    ) {
        MarketEventTemplateResponseDTO template = templateService.createTemplate(
            request
        );
        return ResponseEntity.created(
            URI.create(
                "/api/dashboard/market/event-templates/" + template.templateId()
            )
        ).body(template);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<MarketEventTemplateResponseDTO> updateTemplate(
        @PathVariable String templateId,
        @RequestBody @Valid MarketEventTemplateUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
            templateService.updateTemplate(templateId, request)
        );
    }
}
