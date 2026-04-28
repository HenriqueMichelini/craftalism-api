package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.TransferIncident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferIncidentRepository
    extends JpaRepository<TransferIncident, Long> {}
