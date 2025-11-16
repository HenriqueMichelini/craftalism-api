package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "balances")
public class Balance {

    @Id
    @NotNull(message = "Balance requires a UUID")
    @Column(nullable = false, unique = true)
    private UUID uuid;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Long amount = 0L;

    public Balance() {}

    public Balance(UUID uuid, Long amount) {
        this.uuid = uuid;
        this.amount = amount;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}
