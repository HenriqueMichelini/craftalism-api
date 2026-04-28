package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.PlayerRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.PlayerMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.service.PlayerService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
@Tag(
    name = "Players",
    description = "Operations for managing player information"
)
public class PlayerController {

    private final PlayerService service;
    private final PlayerMapper mapper;

    public PlayerController(PlayerService service, PlayerMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
        summary = "List all players",
        description = "Returns all registered players."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Players retrieved successfully",
                content = @Content(
                    array = @ArraySchema(
                        schema = @Schema(
                            implementation = PlayerResponseDTO.class
                        )
                    )
                )
            ),
        }
    )
    @GetMapping
    public ResponseEntity<List<PlayerResponseDTO>> getAllPlayers() {
        List<Player> players = service.getAllPlayers();
        return ResponseEntity.ok(mapper.toDto(players));
    }

    @Operation(
        summary = "Get player by UUID",
        description = "Retrieves a player's information using their unique UUID."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Player retrieved successfully",
                content = @Content(
                    schema = @Schema(implementation = PlayerResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Player not found for the given UUID"
            ),
        }
    )
    @GetMapping("/{uuid}")
    public ResponseEntity<PlayerResponseDTO> getPlayerByUuid(
        @Parameter(
            description = "Player unique identifier",
            example = "550e8400-e29b-41d4-a716-446655440000"
        ) @PathVariable UUID uuid
    ) {
        Player player = service.getPlayerByUuid(uuid);
        return ResponseEntity.ok(mapper.toDto(player));
    }

    @Operation(
        summary = "Get player by name",
        description = "Retrieves a player's information using their unique in-game name."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "200",
                description = "Player retrieved successfully",
                content = @Content(
                    schema = @Schema(implementation = PlayerResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Player not found for the given name"
            ),
        }
    )
    @GetMapping("/name/{name}")
    public ResponseEntity<PlayerResponseDTO> getPlayerByName(
        @Parameter(
            description = "Player in-game name",
            example = "KOLONY_9"
        ) @PathVariable String name
    ) {
        Player player = service.getPlayerByName(name);
        return ResponseEntity.ok(mapper.toDto(player));
    }

    @Operation(
        summary = "Create player",
        description = "Creates a new player with a UUID and a unique name."
    )
    @ApiResponses(
        {
            @ApiResponse(
                responseCode = "201",
                description = "Player created successfully",
                content = @Content(
                    schema = @Schema(implementation = PlayerResponseDTO.class)
                )
            ),
            @ApiResponse(
                responseCode = "409",
                description = "Player already exists with the given UUID or name"
            ),
            @ApiResponse(
                responseCode = "422",
                description = "Invalid player data"
            ),
        }
    )
    @PostMapping
    public ResponseEntity<PlayerResponseDTO> createPlayer(
        @RequestBody(
            description = "Player data required to create a new player",
            required = true,
            content = @Content(
                schema = @Schema(implementation = PlayerRequestDTO.class)
            )
        ) @Valid @org.springframework.web.bind.annotation.RequestBody PlayerRequestDTO dto
    ) {
        Player created = service.createPlayer(dto.uuid(), dto.name());

        return ResponseEntity.created(
            URI.create("/api/players/" + created.getUuid())
        ).body(mapper.toDto(created));
    }
}
