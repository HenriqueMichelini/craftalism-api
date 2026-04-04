package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.TransferIncidentResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.model.TransferIncident;
import io.github.HenriqueMichelini.craftalism.api.service.TransferIncidentService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfer-incidents")
public class TransferIncidentController {

    private final TransferIncidentService service;

    public TransferIncidentController(TransferIncidentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<TransferIncidentResponseDTO>> getAll() {
        List<TransferIncidentResponseDTO> response = service
            .getAllIncidents()
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(response);
    }

    private TransferIncidentResponseDTO toResponse(TransferIncident incident) {
        return new TransferIncidentResponseDTO(
            incident.getId(),
            incident.getIncidentType(),
            incident.getFromPlayerUuid(),
            incident.getToPlayerUuid(),
            incident.getIdempotencyKey(),
            incident.getReason(),
            incident.getMetadata(),
            incident.getCreatedAt()
        );
    }
}
