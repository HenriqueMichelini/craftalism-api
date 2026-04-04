package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceSetRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceTransferRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceTransferResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.BalanceMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import io.github.HenriqueMichelini.craftalism.api.service.TransferService;
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
    private final TransferService transferService;

    public BalanceController(
        BalanceService service,
        BalanceMapper mapper,
        TransferService transferService
    ) {
        this.service = service;
        this.mapper = mapper;
        this.transferService = transferService;
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
        return ResponseEntity.ok(mapper.toDto(service.getAllBalances()));
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
        return ResponseEntity.ok(mapper.toDto(service.getBalance(uuid)));
    }

    @Operation(
        summary = "Create balance",
        description = "Creates a new player balance. Initial amount must be zero or positive."
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
                responseCode = "400",
                description = "Invalid request body"
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Balance already exists for this UUID"
            ),
        }
    )
    @PostMapping
    public ResponseEntity<BalanceResponseDTO> createBalance(
        @RequestBody @Valid BalanceCreateRequestDTO request
    ) {
        Balance created = service.createBalance(
            request.uuid(),
            request.amount()
        );
        return ResponseEntity.created(
            URI.create("/api/balances/" + created.getUuid())
        ).body(mapper.toDto(created));
    }

    @Operation(
        summary = "Set balance amount",
        description = "Overwrites a player's balance. Amount must be zero or positive."
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
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body"
            ),
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
        @RequestBody @Valid BalanceSetRequestDTO request
    ) {
        Balance updated = service.setBalance(uuid, request.amount());
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @Operation(
        summary = "Deposit funds",
        description = "Adds funds to a player's balance. Amount must be greater than zero."
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
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body"
            ),
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
        @RequestBody @Valid BalanceUpdateRequestDTO request
    ) {
        Balance updated = service.deposit(uuid, request.amount());
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @Operation(
        summary = "Withdraw funds",
        description = "Withdraws funds from a player's balance. Amount must be greater than zero and available."
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
                responseCode = "400",
                description = "Invalid request body"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Balance not found"
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Insufficient funds"
            ),
        }
    )
    @PostMapping("/{uuid}/withdraw")
    public ResponseEntity<BalanceResponseDTO> withdraw(
        @Parameter(
            description = "Player balance UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid,
        @RequestBody @Valid BalanceUpdateRequestDTO request
    ) {
        Balance updated = service.withdraw(uuid, request.amount());
        return ResponseEntity.ok(mapper.toDto(updated));
    }

    @Operation(
        summary = "Transfer funds between players",
        description = "Atomically withdraws from one player and deposits to another in a single request."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Transfer successful"
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid request body"
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Source or destination balance not found"
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Insufficient funds or invalid transfer"
            ),
        }
    )
    @PostMapping("/transfer")
    public ResponseEntity<BalanceTransferResponseDTO> transfer(
        @RequestBody @Valid BalanceTransferRequestDTO request,
        @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        BalanceTransferResponseDTO response = transferService.transfer(
            request.fromPlayerUuid(),
            request.toPlayerUuid(),
            request.amount(),
            idempotencyKey
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get top balances",
        description = "Returns the richest players ordered by balance descending."
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
        return ResponseEntity.ok(mapper.toDto(service.getTopBalances(limit)));
    }
}
