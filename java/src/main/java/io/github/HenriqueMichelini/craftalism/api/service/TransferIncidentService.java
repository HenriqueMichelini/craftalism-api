package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.TransferIncident;
import io.github.HenriqueMichelini.craftalism.api.repository.TransferIncidentRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TransferIncidentService {

    private final TransferIncidentRepository repository;

    public TransferIncidentService(TransferIncidentRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIncident(
        String type,
        UUID from,
        UUID to,
        String idempotencyKey,
        String reason,
        String metadata
    ) {
        TransferIncident incident = new TransferIncident();
        incident.setIncidentType(type);
        incident.setFromPlayerUuid(from);
        incident.setToPlayerUuid(to);
        incident.setIdempotencyKey(idempotencyKey);
        incident.setReason(reason);
        incident.setMetadata(metadata);
        repository.save(incident);
    }

    public List<TransferIncident> getAllIncidents() {
        return repository.findAll();
    }
}
