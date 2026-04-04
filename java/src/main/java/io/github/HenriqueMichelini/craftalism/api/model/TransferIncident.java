package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_incidents")
public class TransferIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String incidentType;

    @Column
    private UUID fromPlayerUuid;

    @Column
    private UUID toPlayerUuid;

    @Column(length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 1024)
    private String reason;

    @Column(length = 4000)
    private String metadata;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public UUID getFromPlayerUuid() {
        return fromPlayerUuid;
    }

    public void setFromPlayerUuid(UUID fromPlayerUuid) {
        this.fromPlayerUuid = fromPlayerUuid;
    }

    public UUID getToPlayerUuid() {
        return toPlayerUuid;
    }

    public void setToPlayerUuid(UUID toPlayerUuid) {
        this.toPlayerUuid = toPlayerUuid;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
