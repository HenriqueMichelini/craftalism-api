package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketRejectionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Authoritative market snapshot, quote, and execute operations")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @Operation(
        summary = "Get market snapshot",
        description = "Returns the authoritative market snapshot used for browsing and stale detection."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Market snapshot returned successfully",
            content = @Content(schema = @Schema(implementation = MarketSnapshotResponseDTO.class))
        ),
    })
    @GetMapping("/snapshot")
    public ResponseEntity<MarketSnapshotResponseDTO> getSnapshot() {
        return ResponseEntity.ok(marketService.getSnapshot());
    }

    @Operation(
        summary = "Create market quote",
        description = "Returns an authoritative quote for a quantity-sensitive market trade."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Quote created successfully",
            content = @Content(schema = @Schema(implementation = MarketQuoteResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Quote rejected because the snapshot or item state is stale",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Quote rejected due to business constraints",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
    })
    @PostMapping("/quotes")
    public ResponseEntity<MarketQuoteResponseDTO> quote(
        JwtAuthenticationToken authentication,
        @RequestHeader(name = "X-Craftalism-Player-Uuid", required = false) String playerUuidHeader,
        @RequestBody(
            description = "Quote request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = MarketQuoteRequestDTO.class))
        ) @Valid @org.springframework.web.bind.annotation.RequestBody MarketQuoteRequestDTO request
    ) {
        return ResponseEntity.ok(marketService.quote(authentication, request, playerUuidHeader));
    }

    @Operation(
        summary = "Execute market trade",
        description = "Executes a quote-backed market trade and returns the updated item state."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Trade executed successfully",
            content = @Content(schema = @Schema(implementation = MarketExecuteSuccessResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Execution rejected because the quote is stale or expired",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Execution rejected due to business constraints",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
    })
    @PostMapping("/execute")
    public ResponseEntity<MarketExecuteSuccessResponseDTO> execute(
        JwtAuthenticationToken authentication,
        @RequestHeader(name = "X-Craftalism-Player-Uuid", required = false) String playerUuidHeader,
        @RequestBody(
            description = "Execution request payload",
            required = true,
            content = @Content(schema = @Schema(implementation = MarketExecuteRequestDTO.class))
        ) @Valid @org.springframework.web.bind.annotation.RequestBody MarketExecuteRequestDTO request
    ) {
        return ResponseEntity.ok(marketService.execute(authentication, request, playerUuidHeader));
    }
}
