package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.BalanceMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/balances")
@Tag(name = "Balances", description = "Balance management for players")
public class BalanceController {
    private final BalanceService service;
    private final BalanceMapper mapper;

    public BalanceController(BalanceService service, BalanceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Get balance by UUID",
            description = "Returns the balance information for a specific player UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance found successfully",
                    content = @Content(schema = @Schema(implementation = BalanceResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Balance not found for this UUID"
            )
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<BalanceResponseDTO> getBalanceByUuid(
            @Parameter(description = "Player UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID uuid) {
        Balance balance = service.getBalance(uuid);

        return ResponseEntity
                .ok(mapper.toDto(balance));
    }

    @Operation(
            summary = "Create new balance",
            description = "Creates a new balance for a player with initial amount of 0"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Balance created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Balance already exists for this UUID"
            )
    })
    @PostMapping()
    public ResponseEntity<BalanceResponseDTO> createBalance(
            @Parameter(description = "Balance UUID", required = true)
            @RequestParam UUID uuid) {
        Balance created = service.createBalance(uuid);

        return ResponseEntity
                .created(URI.create("/api/balances/" + created.getUuid()))
                .body(mapper.toDto(created));
    }

    @Operation(
            summary = "Update balance amount",
            description = "Updates the balance amount for a specific player. Amount must be non-negative."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Balance updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid amount (negative value)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Balance not found for this UUID"
            )
    })
    @PutMapping("/{uuid}")
    public ResponseEntity<BalanceResponseDTO> updateBalance(
            @Parameter(description = "Balance UUID")
            @PathVariable UUID uuid,

            @Parameter(description = "New balance amount (must be >= 0)", example = "1000")
            @RequestParam Long amount) {
        Balance updated = service.updateBalance(uuid, amount);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @GetMapping("/top")
    public ResponseEntity<List<BalanceResponseDTO>> getTopBalances(
        @Parameter(description = "Balance limit number (must be >= 0 and <= 20)", example = "15")
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<Balance> topBalances = service.getTopBalances(limit);

        List<BalanceResponseDTO> response = topBalances.stream()
                .map(balance -> new BalanceResponseDTO(
                        balance.getUuid(),
                        balance.getAmount()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}