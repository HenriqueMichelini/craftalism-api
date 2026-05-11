package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketTradeHistoryNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(MarketTradeHistoryReadService.class)
class MarketTradeHistoryReadServiceTest {

    private static final UUID PLAYER_UUID = UUID.fromString(
        "110e8400-e29b-41d4-a716-446655440000"
    );

    @Autowired
    private MarketTradeHistoryReadService service;

    @Autowired
    private MarketTradeHistoryRepository repository;

    @Test
    void findTrades_filtersComposablyAndUsesDefaultNewestOrdering() {
        MarketTradeHistory olderMatch = repository.save(
            history(PLAYER_UUID, "wheat", MarketSide.BUY, Instant.parse("2026-05-01T10:00:00Z"))
        );
        MarketTradeHistory newerMatch = repository.save(
            history(PLAYER_UUID, "wheat", MarketSide.BUY, Instant.parse("2026-05-01T11:00:00Z"))
        );
        repository.save(
            history(PLAYER_UUID, "wheat", MarketSide.SELL, Instant.parse("2026-05-01T11:30:00Z"))
        );
        repository.save(
            history(UUID.fromString("220e8400-e29b-41d4-a716-446655440000"), "wheat", MarketSide.BUY, Instant.parse("2026-05-01T12:00:00Z"))
        );
        repository.save(
            history(PLAYER_UUID, "carrot", MarketSide.BUY, Instant.parse("2026-05-01T12:30:00Z"))
        );

        Page<MarketTradeHistoryDTO> page = service.findTrades(
            new MarketTradeHistoryFilterDTO(
                PLAYER_UUID,
                "wheat",
                MarketSide.BUY,
                Instant.parse("2026-05-01T10:00:00Z"),
                Instant.parse("2026-05-01T11:00:00Z")
            ),
            PageRequest.of(0, 20)
        );

        assertEquals(2, page.getTotalElements());
        assertEquals(newerMatch.getId(), page.getContent().get(0).id());
        assertEquals(olderMatch.getId(), page.getContent().get(1).id());
    }

    @Test
    void getTrade_returnsDetailDto() {
        MarketTradeHistory history = repository.save(
            history(PLAYER_UUID, "wheat", MarketSide.SELL, Instant.parse("2026-05-01T10:00:00Z"))
        );

        MarketTradeHistoryDTO dto = service.getTrade(history.getId());

        assertEquals(history.getId(), dto.id());
        assertEquals(PLAYER_UUID, dto.playerUuid());
        assertEquals("wheat", dto.itemId());
        assertEquals(MarketSide.SELL, dto.side());
        assertEquals("5", dto.unitPrice());
        assertEquals("50", dto.totalPrice());
    }

    @Test
    void getTrade_unknownIdThrowsNotFound() {
        assertThrows(
            MarketTradeHistoryNotFoundException.class,
            () -> service.getTrade(404L)
        );
    }

    private MarketTradeHistory history(
        UUID playerUuid,
        String itemId,
        MarketSide side,
        Instant executedAt
    ) {
        MarketTradeHistory history = new MarketTradeHistory();
        history.setPlayerUuid(playerUuid);
        history.setItemId(itemId);
        history.setSide(side);
        history.setQuantity(10L);
        history.setUnitPrice(5L);
        history.setTotalPrice(50L);
        history.setCurrency("coins");
        history.setSnapshotVersion("market:snapshot");
        history.setExecutedAt(executedAt);
        return history;
    }
}
