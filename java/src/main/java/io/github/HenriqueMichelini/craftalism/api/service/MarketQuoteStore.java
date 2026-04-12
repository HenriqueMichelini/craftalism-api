package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class MarketQuoteStore {

    private final Map<String, StoredQuote> quotes = new ConcurrentHashMap<>();

    public void put(StoredQuote quote) {
        quotes.put(quote.quoteToken(), quote);
    }

    public Optional<StoredQuote> getActive(String quoteToken) {
        StoredQuote quote = quotes.get(quoteToken);
        if (quote == null) {
            return Optional.empty();
        }
        if (quote.expiresAt().isBefore(Instant.now())) {
            quotes.remove(quoteToken);
            return Optional.empty();
        }
        return Optional.of(quote);
    }

    public void remove(String quoteToken) {
        quotes.remove(quoteToken);
    }

    public void clear() {
        quotes.clear();
    }

    public record StoredQuote(
        String quoteToken,
        UUID playerUuid,
        String itemId,
        MarketSide side,
        long quantity,
        long unitPrice,
        long totalPrice,
        String snapshotVersion,
        Instant expiresAt
    ) {}
}
