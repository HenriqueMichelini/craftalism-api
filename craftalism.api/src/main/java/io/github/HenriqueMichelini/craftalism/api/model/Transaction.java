package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Transaction requires a sender UUID")
    @Column(name = "from_uuid", nullable = false)
    private UUID fromUuid;

    @NotNull(message = "Transaction requires a receiver UUID")
    @Column(name = "to_uuid", nullable = false)
    private UUID toUuid;

    @NotNull(message = "Transaction requires an amount")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private final Instant createdAt = Instant.now();

    public Transaction() {}

    public Transaction(UUID fromUuid, UUID toUuid, BigDecimal amount) {
        this.fromUuid = fromUuid;
        this.toUuid = toUuid;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public UUID getFromUuid() { return fromUuid; }
    public UUID getToUuid() { return toUuid; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFromUuid(UUID fromUuid) { this.fromUuid = fromUuid; }
    public void setToUuid(UUID toUuid) { this.toUuid = toUuid; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
