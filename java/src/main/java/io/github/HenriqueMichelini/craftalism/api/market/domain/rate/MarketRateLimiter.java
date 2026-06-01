package io.github.HenriqueMichelini.craftalism.api.market.domain.rate;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MarketRateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentMap<UUID, Counter> counters = new ConcurrentHashMap<>();

    public MarketRateLimiter(int maxRequests, Duration window, Clock clock) {
        this.maxRequests = maxRequests;
        this.windowMillis = Math.max(1L, window.toMillis());
        this.clock = clock;
    }

    public boolean tryAcquire(UUID playerUuid) {
        if (maxRequests <= 0) {
            return true;
        }

        long window = Math.floorDiv(clock.millis(), windowMillis);
        Counter counter = counters.compute(
            playerUuid,
            (ignored, existing) -> {
                if (existing == null || existing.window() != window) {
                    return new Counter(window, 1);
                }
                return new Counter(window, existing.count() + 1);
            }
        );
        return counter.count() <= maxRequests;
    }

    private record Counter(long window, int count) {}
}
