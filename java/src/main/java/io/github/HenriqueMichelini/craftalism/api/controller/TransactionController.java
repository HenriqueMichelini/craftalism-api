package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/transactions")
@Tag(
    name = "Transactions",
    description = "Operations for managing transfers between player balances"
)
@Validated
public class TransactionController {

    private final TransactionService service;
    private final TransactionMapper mapper;

    public TransactionController(
        TransactionService service,
        TransactionMapper mapper
    ) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "List all transactions",
        description = "Returns all transactions recorded in the system."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Transactions retrieved successfully",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(
                            implementation = TransactionResponseDTO.class
                        )
                    )
                )
            ),
        }
    )
    @GetMapping
    public ResponseEntity<List<TransactionResponseDTO>> getAllTransactions() {
        List<Transaction> transactions = service.getAllTransactions();
        return ResponseEntity.ok(mapper.toDto(transactions));
    }

    @Operation(
        summary = "Get transaction by ID",
        description = "Retrieves a specific transaction using its unique identifier."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Transaction retrieved successfully",
                content = @Content(
                    schema = @Schema(
                        implementation = TransactionResponseDTO.class
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Transaction not found"
            ),
        }
    )
    @GetMapping("/id/{id}")
    public ResponseEntity<TransactionResponseDTO> getTransactionById(
        @Parameter(
            description = "Transaction identifier",
            example = "123"
        ) @PathVariable Long id
    ) {
        Transaction tx = service.getTransactionById(id);
        return ResponseEntity.ok(mapper.toDto(tx));
    }

    @Operation(
        summary = "Get transactions sent by player",
        description = "Returns all transactions where the specified player was the sender."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Transactions retrieved successfully",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(
                            implementation = TransactionResponseDTO.class
                        )
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Player not found"
            ),
        }
    )
    @GetMapping("/from/{uuid}")
    public ResponseEntity<
        List<TransactionResponseDTO>
    > getTransactionsByFromUuid(
        @Parameter(
            description = "Sender player UUID",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid
    ) {
        List<TransactionResponseDTO> list = service
            .getTransactionsByFromUuid(uuid)
            .stream()
            .map(mapper::toDto)
            .toList();

        return ResponseEntity.ok(list);
    }

    @Operation(
        summary = "Get transactions received by player",
        description = "Returns all transactions where the specified player was the receiver."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Transactions retrieved successfully",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(
                            implementation = TransactionResponseDTO.class
                        )
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Player not found"
            ),
        }
    )
    @GetMapping("/to/{uuid}")
    public ResponseEntity<List<TransactionResponseDTO>> getTransactionsByToUuid(
        @Parameter(
            description = "Receiver player UUID",
            example = "550e8400-e29b-41d4-a716-446655440001"
        ) @PathVariable UUID uuid
    ) {
        List<TransactionResponseDTO> list = service
            .getTransactionsByToUuid(uuid)
            .stream()
            .map(mapper::toDto)
            .toList();

        return ResponseEntity.ok(list);
    }

    @Operation(
        summary = "Create transaction",
        description = "Creates a new transaction transferring an amount from one player balance to another."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "201",
                description = "Transaction created successfully",
                content = @Content(
                    schema = @Schema(
                        implementation = TransactionResponseDTO.class
                    )
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Sender or receiver balance not found"
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Invalid transaction data"
            ),
        }
    )
    @PostMapping
    public ResponseEntity<TransactionResponseDTO> createTransaction(
        @RequestBody(
            description = "Transaction data including sender UUID, receiver UUID and transfer amount",
            required = true,
            content = @Content(
                schema = @Schema(implementation = TransactionRequestDTO.class)
            )
        ) @Valid @org.springframework.web.bind.annotation.RequestBody TransactionRequestDTO dto
    ) {
        TransactionResponseDTO created = service.processTransaction(dto);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/id/{id}")
            .buildAndExpand(created.id())
            .toUri();

        return ResponseEntity.created(location).body(created);
    }
}
