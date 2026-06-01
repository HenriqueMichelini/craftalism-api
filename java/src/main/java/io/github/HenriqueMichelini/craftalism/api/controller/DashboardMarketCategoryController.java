package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketCategoryCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketCategoryResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketCategoryUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.MarketCategoryMapper;
import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.DashboardMarketCategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/market/categories")
public class DashboardMarketCategoryController {

    private final DashboardMarketCategoryService service;
    private final MarketCategoryMapper mapper;

    public DashboardMarketCategoryController(
        DashboardMarketCategoryService service,
        MarketCategoryMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<MarketCategoryResponseDTO>> getAllMarketCategories() {
        return ResponseEntity.ok(mapper.toDto(service.getAllMarketCategories()));
    }

    @PostMapping
    public ResponseEntity<MarketCategoryResponseDTO> createMarketCategory(
        @RequestBody @Valid MarketCategoryCreateRequestDTO request
    ) {
        MarketCategory created = service.createMarketCategory(request);
        return ResponseEntity.created(
            URI.create(
                "/api/dashboard/market/categories/" + created.getCategoryId()
            )
        ).body(mapper.toDto(created));
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<MarketCategoryResponseDTO> updateMarketCategory(
        @PathVariable String categoryId,
        @RequestBody @Valid MarketCategoryUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
            mapper.toDto(service.updateMarketCategory(categoryId, request))
        );
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteMarketCategory(
        @PathVariable String categoryId
    ) {
        service.deleteMarketCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}
