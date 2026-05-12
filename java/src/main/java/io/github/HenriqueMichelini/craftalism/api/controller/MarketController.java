package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketRejectionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import io.github.HenriqueMichelini.craftalism.api.service.MarketTradeHistoryReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Authoritative market snapshot, quote, and execute operations")
public class MarketController {

    private final MarketService marketService;
    private final MarketTradeHistoryReadService tradeHistoryReadService;

    public MarketController(
        MarketService marketService,
        MarketTradeHistoryReadService tradeHistoryReadService
    ) {
        this.marketService = marketService;
        this.tradeHistoryReadService = tradeHistoryReadService;
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
        summary = "List market trades",
        description = "Returns committed successful market executions."
    )
    @GetMapping("/trades")
    public ResponseEntity<Page<MarketTradeHistoryDTO>> getTrades(
        @RequestParam(required = false) String playerUuid,
        @RequestParam(required = false) String playerUuidMatch,
        @RequestParam(required = false) String itemId,
        @RequestParam(required = false) String itemIdMatch,
        @RequestParam(required = false) MarketSide side,
        @RequestParam(required = false) Long minTotalPrice,
        @RequestParam(required = false) Long maxTotalPrice,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant executedFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant executedTo,
        Pageable pageable
    ) {
        MarketTradeHistoryFilterDTO filter = new MarketTradeHistoryFilterDTO(
            playerUuid,
            playerUuidMatch,
            itemId,
            itemIdMatch,
            side,
            minTotalPrice,
            maxTotalPrice,
            executedFrom,
            executedTo
        );
        return ResponseEntity.ok(tradeHistoryReadService.findTrades(filter, pageable));
    }

    @Operation(
        summary = "Get market trade",
        description = "Returns one committed successful market execution."
    )
    @GetMapping("/trades/{id}")
    public ResponseEntity<MarketTradeHistoryDTO> getTrade(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(tradeHistoryReadService.getTrade(id));
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
            responseCode = "404",
            description = "Quote rejected because the market item does not exist",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Quote rejected due to business constraints",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Quote rejected because the market is closed or authenticated player context is unavailable",
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
            responseCode = "404",
            description = "Execution rejected because the market item does not exist",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Execution rejected due to business constraints",
            content = @Content(schema = @Schema(implementation = MarketRejectionResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "503",
            description = "Execution rejected because the market is closed or authenticated player context is unavailable",
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
