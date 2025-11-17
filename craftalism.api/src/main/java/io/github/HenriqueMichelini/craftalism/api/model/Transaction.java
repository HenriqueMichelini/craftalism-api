package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "from_uuid", referencedColumnName = "uuid", nullable = false)
    private final UUID fromUuid;

    @JoinColumn(name = "to_uuid", referencedColumnName = "uuid", nullable = false)
    private final UUID toUuid;

    @NotNull(message = "Transaction requires an amount")
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Transaction(UUID fromUuid, UUID toUuid, Long amount) {
        this.fromUuid = fromUuid;
        this.toUuid = toUuid;
        this.amount = amount;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getFromUuid() { return fromUuid; }
    public UUID getToUuid() { return toUuid; }
    public Long getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAmount(Long amount) { this.amount = amount; }
}

