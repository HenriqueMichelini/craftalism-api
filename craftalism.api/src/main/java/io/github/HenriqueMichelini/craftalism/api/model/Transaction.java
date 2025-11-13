package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_uuid", referencedColumnName = "uuid", nullable = false)
    private Balance fromBalance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_uuid", referencedColumnName = "uuid", nullable = false)
    private Balance toBalance;

    @NotNull(message = "Transaction requires an amount")
    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Transaction() {}

    public Transaction(Balance fromBalance, Balance toBalance, Long amount) {
        this.fromBalance = fromBalance;
        this.toBalance = toBalance;
        this.amount = amount;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    // Getters & setters
    public Long getId() { return id; }
    public Balance getFromBalance() { return fromBalance; }
    public Balance getToBalance() { return toBalance; }
    public Long getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFromBalance(Balance fromBalance) { this.fromBalance = fromBalance; }
    public void setToBalance(Balance toBalance) { this.toBalance = toBalance; }
    public void setAmount(Long amount) { this.amount = amount; }
}

