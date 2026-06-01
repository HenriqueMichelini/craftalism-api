package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.store;

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
        expireActiveQuotesInCurrentTransaction();

        MarketQuote entity = new MarketQuote();
        entity.setQuoteToken(quote.quoteToken());
        entity.setPlayerUuid(quote.playerUuid());
        entity.setItemId(quote.itemId());
        entity.setSide(quote.side());
        entity.setQuantity(quote.quantity());
        entity.setUnitPrice(quote.unitPrice());
        entity.setTotalPrice(quote.totalPrice());
        entity.setSnapshotVersion(quote.snapshotVersion());
        entity.setPricingContextVersion(quote.pricingContextVersion());
        entity.setPressurePosition(quote.pressurePosition());
        entity.setDriftRevision(quote.driftRevision());
        entity.setNamedEventInstanceId(quote.namedEventInstanceId());
        entity.setEventEffectVersion(quote.eventEffectVersion());
        entity.setExpiresAt(quote.expiresAt());
        entity.setCreatedAt(Instant.now());
        entity.setStatus(MarketQuote.Status.ACTIVE);
        marketQuoteRepository.save(entity);
    }

    @Transactional
    public Optional<StoredQuote> get(String quoteToken) {
        expireActiveQuotesInCurrentTransaction();
        return marketQuoteRepository.findByQuoteToken(quoteToken).map(this::toStoredQuote);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean consume(String quoteToken) {
        expireActiveQuotesInCurrentTransaction();
        return (
            marketQuoteRepository.transitionStatus(
                quoteToken,
                MarketQuote.Status.ACTIVE,
                MarketQuote.Status.CONSUMED,
                Instant.now()
            ) == 1
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidate(String quoteToken) {
        marketQuoteRepository.transitionStatus(
            quoteToken,
            MarketQuote.Status.ACTIVE,
            MarketQuote.Status.INVALIDATED,
            Instant.now()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(String quoteToken) {
        marketQuoteRepository.transitionStatus(
            quoteToken,
            MarketQuote.Status.ACTIVE,
            MarketQuote.Status.EXPIRED,
            Instant.now()
        );
    }

    @Transactional
    public void clear() {
        marketQuoteRepository.deleteAll();
    }

    @Transactional
    public void expireActiveQuotes() {
        expireActiveQuotesInCurrentTransaction();
    }

    private void expireActiveQuotesInCurrentTransaction() {
        Instant now = Instant.now();
        marketQuoteRepository.expireActiveQuotes(
            now,
            MarketQuote.Status.ACTIVE,
            MarketQuote.Status.EXPIRED,
            now
        );
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
            quote.getPricingContextVersion(),
            quote.getPressurePosition(),
            quote.getDriftRevision(),
            quote.getNamedEventInstanceId(),
            quote.getEventEffectVersion(),
            quote.getExpiresAt(),
            quote.getStatus()
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
        int pricingContextVersion,
        long pressurePosition,
        Long driftRevision,
        Long namedEventInstanceId,
        Integer eventEffectVersion,
        Instant expiresAt,
        MarketQuote.Status status
    ) {}
}
