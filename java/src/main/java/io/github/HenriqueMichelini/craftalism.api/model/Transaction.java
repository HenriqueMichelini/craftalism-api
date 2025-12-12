package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "transactions")
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_player_uuid")
    private UUID fromPlayerUuid;

    @Column(name = "to_player_uuid")
    private UUID toPlayerUuid;

    @NotNull(message = "Transaction requires an amount")
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;


    public Transaction(UUID fromPlayerUuid, UUID toPlayerUuid, Long amount) {
        this.fromPlayerUuid = fromPlayerUuid;
        this.toPlayerUuid = toPlayerUuid;
        this.amount = amount;
    }

    protected Transaction() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getFromPlayerUuid() { return fromPlayerUuid; }
    public UUID getToPlayerUuid() { return toPlayerUuid; }
    public Long getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAmount(Long amount) { this.amount = amount; }
}

