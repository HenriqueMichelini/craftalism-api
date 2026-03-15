package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.BalanceMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/balances")
@Tag(name = "Balances", description = "Operations for managing player balances")
public class BalanceController {

    private final BalanceService service;
    private final BalanceMapper mapper;

    public BalanceController(BalanceService service, BalanceMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "List all balances",
        description = "Returns all player balances stored in the system."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Balances retrieved successfully",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(
                            implementation = BalanceResponseDTO.class
                        )
                    )
                )
            ),
        }
    )
    @GetMapping
    public ResponseEntity<List<BalanceResponseDTO>> getAllBalances() {
        List<Balance> balances = service.getAllBalances();
        return ResponseEntity.ok(mapper.toDto(balances));
    }

    @Operation(
        summary = "Get balance by UUID",
        description = "Retrieves the balance for a specific player identified by UUID."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Balance retrieved successfully",
                content = @Content(
                    schema = @Schema(implementation = BalanceResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Balance not found for the given UUID"
            ),
        }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<BalanceResponseDTO> getBalanceByUuid(
        @Parameter(
            description = "Player unique identifier",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid
    ) {
        Balance balance = service.getBalance(uuid);
        return ResponseEntity.ok(mapper.toDto(balance));
    }

    @Operation(
        summary = "Create balance",
        description = "Creates a new player balance with an initial amount of 0."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "201",
                description = "Balance created successfully",
                content = @Content(
                    schema = @Schema(implementation = BalanceResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Balance already exists for this UUID"
            ),
        }
    )
    @PostMapping
    public ResponseEntity<BalanceResponseDTO> createBalance(
        @RequestBody @Valid BalanceRequestDTO request
    ) {
        Balance created = service.createBalance(request.uuid());

        return ResponseEntity.created(
            URI.create("/api/balances/" + created.getUuid())
        ).body(mapper.toDto(created));
    }

    @Operation(
        summary = "Set balance amount",
        description = "Sets the balance amount for a specific player. The amount must be non-negative."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Balance updated successfully",
                content = @Content(
                    schema = @Schema(implementation = BalanceResponseDTO.class)
                )
            ),
            @ApiResponse(responseCode = "422", description = "Invalid amount"),
            @ApiResponse(
                responseCode = "404",
                description = "Balance not found"
            ),
        }
    )
    @PutMapping("/{uuid}/set")
    public ResponseEntity<BalanceResponseDTO> setBalance(
        @Parameter(
            description = "Player balance UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid,
        @Parameter(
            description = "New balance amount (must be >= 0)",
            example = "1000"
        ) @RequestParam Long amount
    ) {
        Balance updated = service.setBalance(uuid, amount);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @Operation(
        summary = "Deposit funds",
        description = "Adds funds to a player's balance. The amount must be greater than zero."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Deposit successful",
                content = @Content(
                    schema = @Schema(implementation = BalanceResponseDTO.class)
                )
            ),
            @ApiResponse(responseCode = "422", description = "Invalid amount"),
            @ApiResponse(
                responseCode = "404",
                description = "Balance not found"
            ),
        }
    )
    @PostMapping("/{uuid}/deposit")
    public ResponseEntity<BalanceResponseDTO> deposit(
        @Parameter(
            description = "Player balance UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid,
        @Parameter(
            description = "Amount to deposit",
            example = "500"
        ) @RequestParam long amount
    ) {
        Balance updated = service.deposit(uuid, amount);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @Operation(
        summary = "Withdraw funds",
        description = "Withdraws funds from a player's balance. The amount must be greater than zero and available."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Withdrawal successful",
                content = @Content(
                    schema = @Schema(implementation = BalanceResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Balance not found"
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Insufficient funds"
            ),
            @ApiResponse(responseCode = "422", description = "Invalid amount"),
        }
    )
    @PostMapping("/{uuid}/withdraw")
    public ResponseEntity<BalanceResponseDTO> withdraw(
        @Parameter(
            description = "Player balance UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid,
        @Parameter(
            description = "Amount to withdraw",
            example = "200"
        ) @RequestParam long amount
    ) {
        Balance updated = service.withdraw(uuid, amount);
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @Operation(
        summary = "Get top balances",
        description = "Returns the richest player balances ordered by amount in descending order."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Top balances retrieved successfully",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(
                            implementation = BalanceResponseDTO.class
                        )
                    )
                )
            ),
        }
    )
    @GetMapping("/top")
    public ResponseEntity<List<BalanceResponseDTO>> getTopBalances(
        @Parameter(
            description = "Maximum number of balances to return (1–20)",
            example = "10"
        ) @RequestParam(defaultValue = "10") int limit
    ) {
        List<Balance> balances = service.getTopBalances(limit);
        return ResponseEntity.ok(mapper.toDto(balances));
    }
}
