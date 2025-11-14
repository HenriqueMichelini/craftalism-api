package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.PlayerRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.PlayerMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@Tag(name = "Player", description = "Player data management")
public class PlayerController {

    private final PlayerService service;
    private final PlayerMapper mapper;

    public PlayerController(PlayerService service, PlayerMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Get Player by UUID",
            description = "Returns the player information for a specific UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Player found successfully",
                    content = @Content(schema = @Schema(implementation = PlayerResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Player not found for this UUID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{uuid}")
    public ResponseEntity<PlayerResponseDTO> getPlayerByUuid(
            @Parameter(description = "Player UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID uuid
    ) {
        Player player = service.getPlayerByUuid(uuid);

        return ResponseEntity
                .ok(mapper.toDto(player));
    }

    @Operation(
            summary = "Get Player by name",
            description = "Returns the player information for a specific name"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Player found successfully",
                    content = @Content(schema = @Schema(implementation = PlayerResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Player not found for this name",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/name/{name}")
    public ResponseEntity<PlayerResponseDTO> getPlayerByName(
            @Parameter(description = "Player name", example = "KOLONY_9")
            @PathVariable String name
    ) {
        Player player = service.getPlayerByName(name);

        return ResponseEntity
                .ok(mapper.toDto(player));
    }

    @Operation(
            summary = "Create new player",
            description = "Creates a new player with it's name and UUID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Player created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Player already exists for this UUID"
            )
    })
    @PostMapping
    public ResponseEntity<PlayerResponseDTO> createPlayer(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Player data",
                    required = true
            )
            @RequestBody @Valid PlayerRequestDTO dto
    ) {
        Player created = service.createPlayer(dto.uuid(), dto.name());

        return ResponseEntity
                .created(URI.create("/api/players/" + created.getUuid()))
                .body(mapper.toDto(created));
    }
}
