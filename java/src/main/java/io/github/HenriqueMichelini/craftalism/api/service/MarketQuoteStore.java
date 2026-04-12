package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MarketQuoteStore {

    private final MarketQuoteRepository marketQuoteRepository;

    public MarketQuoteStore(MarketQuoteRepository marketQuoteRepository) {
        this.marketQuoteRepository = marketQuoteRepository;
    }

    @Transactional
    public void put(StoredQuote quote) {
        deleteExpired();

        MarketQuote entity = new MarketQuote();
        entity.setQuoteToken(quote.quoteToken());
        entity.setPlayerUuid(quote.playerUuid());
        entity.setItemId(quote.itemId());
        entity.setSide(quote.side());
        entity.setQuantity(quote.quantity());
        entity.setUnitPrice(quote.unitPrice());
        entity.setTotalPrice(quote.totalPrice());
        entity.setSnapshotVersion(quote.snapshotVersion());
        entity.setExpiresAt(quote.expiresAt());
        entity.setCreatedAt(Instant.now());
        marketQuoteRepository.save(entity);
    }

    @Transactional
    public Optional<StoredQuote> getActive(String quoteToken) {
        Optional<MarketQuote> quote = marketQuoteRepository.findById(quoteToken);
        if (quote.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toStoredQuote(quote.get()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(String quoteToken) {
        marketQuoteRepository.deleteById(quoteToken);
    }

    @Transactional
    public void clear() {
        marketQuoteRepository.deleteAll();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteExpired() {
        marketQuoteRepository.deleteExpired(Instant.now());
    }

    private StoredQuote toStoredQuote(MarketQuote quote) {
        return new StoredQuote(
            quote.getQuoteToken(),
            quote.getPlayerUuid(),
            quote.getItemId(),
            quote.getSide(),
            quote.getQuantity(),
            quote.getUnitPrice(),
            quote.getTotalPrice(),
            quote.getSnapshotVersion(),
            quote.getExpiresAt()
        );
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
