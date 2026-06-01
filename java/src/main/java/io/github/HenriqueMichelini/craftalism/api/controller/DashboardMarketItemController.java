package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketItemCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketItemResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketItemUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.MarketItemMapper;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.DashboardMarketItemService;
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
@RequestMapping("/api/dashboard/market/items")
public class DashboardMarketItemController {

    private final DashboardMarketItemService service;
    private final MarketItemMapper mapper;

    public DashboardMarketItemController(
        DashboardMarketItemService service,
        MarketItemMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<MarketItemResponseDTO>> getAllMarketItems() {
        return ResponseEntity.ok(mapper.toDto(service.getAllMarketItems()));
    }

    @PostMapping
    public ResponseEntity<MarketItemResponseDTO> createMarketItem(
        @RequestBody @Valid MarketItemCreateRequestDTO request
    ) {
        MarketItem created = service.createMarketItem(request);
        return ResponseEntity.created(
            URI.create("/api/dashboard/market/items/" + created.getItemId())
        ).body(mapper.toDto(created));
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<MarketItemResponseDTO> updateMarketItem(
        @PathVariable String itemId,
        @RequestBody @Valid MarketItemUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
            mapper.toDto(service.updateMarketItem(itemId, request))
        );
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteMarketItem(@PathVariable String itemId) {
        service.deleteMarketItem(itemId);
        return ResponseEntity.noContent().build();
    }
}
