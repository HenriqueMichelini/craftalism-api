package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSchedulerLock;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventSchedulerLockRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
        "craftalism.market-events.scheduler.enabled=true",
        "craftalism.market.enabled=true",
        "craftalism.market-events.scheduler.start-chance-basis-points=0",
        "craftalism.market-events.scheduler.initial-delay-ms=3600000",
    }
)
@ActiveProfiles("local")
class MarketEventSchedulerSpringTransactionTest {

    @Autowired
    private MarketEventScheduler scheduler;

    @Autowired
    private MarketEventSchedulerLockRepository lockRepository;

    @Autowired
    private MarketEventInstanceRepository eventRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        lockRepository.deleteAll();
        lockRepository.save(expiredSchedulerLock());
    }

    @Test
    void scheduledRoll_acquiresLeaseInsideTransaction() {
        scheduler.scheduledRoll();

        MarketEventSchedulerLock lock = lockRepository
            .findById("market_event_scheduler")
            .orElseThrow();
        assertNotEquals("bootstrap", lock.getOwner());
        assertTrue(lock.getLeaseUntil().isAfter(Instant.EPOCH));
    }

    private MarketEventSchedulerLock expiredSchedulerLock() {
        MarketEventSchedulerLock lock = new MarketEventSchedulerLock();
        lock.setLockName("market_event_scheduler");
        lock.setOwner("bootstrap");
        lock.setLeaseUntil(Instant.EPOCH);
        lock.setUpdatedAt(Instant.EPOCH);
        return lock;
    }
}
