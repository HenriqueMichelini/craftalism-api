package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import io.github.HenriqueMichelini.craftalism.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Transaction management between player balances")
public class TransactionController {
    private final TransactionService service;
    private final TransactionMapper mapper;

    public TransactionController(TransactionService service, BalanceService balanceService, TransactionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Get all transactions",
            description = "Returns a list of all transactions in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDTO.class))
            )
    })
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getAllTransactions() {
        List<TransactionResponseDTO> list = service.getAllTransactions()
                .stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity
                .ok(list);
    }

    @Operation(
            summary = "Get transaction by ID",
            description = "Returns a specific transaction by its unique identifier"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transaction found successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Transaction not found for this ID"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
            @Parameter(description = "Transaction ID", example = "123")
            @PathVariable Long id
    ) {
        Transaction transaction = service.getTransactionById(id);

        return ResponseEntity.ok(mapper.toDto(transaction));
    }

    @Operation(
            summary = "Get transactions sent by player",
            description = "Returns all transactions where the specified player was the sender"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Balance not found for this UUID"
            )
    })
    @GetMapping("/from/{uuid}")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionsByFromUuid(
            @Parameter(description = "Sender's player UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID uuid
    ) {
        List<TransactionResponseDTO> list = service.getTransactionByFromBalance(uuid)
                .stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Get transactions received by player",
            description = "Returns all transactions where the specified player was the receiver"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transactions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Balance not found for this UUID"
            )
    })
    @GetMapping("/to/{uuid}")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionsByToUuid(
            @Parameter(description = "Receiver's player UUID", example = "550e8400-e29b-41d4-a716-446655440001")
            @PathVariable UUID uuid
    ) {
        List<TransactionResponseDTO> list = service.getTransactionByToBalance(uuid)
                .stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Create new transaction",
            description = "Creates a new transaction transferring amount from one player to another"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Transaction created successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid transaction (insufficient balance, invalid amount, etc.)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "One or both balances not found"
            )
    })
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Transaction data with sender, receiver and amount",
                required = true,
                content = @Content(schema = @Schema(implementation = TransactionRequestDTO.class))
            )
            @RequestBody @Valid TransactionRequestDTO dto
        ) {
            TransactionResponseDTO created = service.processTransaction(dto);

            return ResponseEntity
                .created(URI.create("/api/transactions/" + created.id()))
                .body(created);
    }
}

