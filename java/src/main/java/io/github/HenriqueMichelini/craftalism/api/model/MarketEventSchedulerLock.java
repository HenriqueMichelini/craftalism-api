package io.github.HenriqueMichelini.craftalism.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity(name = "market_event_scheduler_locks")
@Table(name = "market_event_scheduler_locks")
public class MarketEventSchedulerLock {

    @Id
    @Column(nullable = false, updatable = false, length = 64)
    private String lockName;

    @Column(nullable = false, length = 128)
    private String owner;

    @Column(nullable = false)
    private Instant leaseUntil;

    @Column(nullable = false)
    private Instant updatedAt;

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Instant getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(Instant leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
