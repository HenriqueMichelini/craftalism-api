package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MarketQuoteServiceTest {

    @Mock
    private MarketItemRepository marketItemRepository;

    @Mock
    private MarketQuoteStore quoteStore;

    private MarketSnapshotService marketSnapshotService;
    private MarketQuoteService marketQuoteService;

    @BeforeEach
    void setUp() {
        MarketTradePlanner tradePlanner = new MarketTradePlanner();
        marketSnapshotService = new MarketSnapshotService(
            new MarketReadService(marketItemRepository, tradePlanner),
            new MarketSnapshotProjector(tradePlanner)
        );
        marketQuoteService = new MarketQuoteService(
            marketSnapshotService,
            quoteStore,
            tradePlanner,
            new MarketPlayerResolver("minecraft-server"),
            new MarketRateLimiter(0, Duration.ofSeconds(60L), fixedClock()),
            true,
            60L
        );
    }

    @Test
    void quote_storesSameQuoteValuesReturnedToClient() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(List.of(item));
        String snapshotVersion = marketSnapshotService
            .getSnapshot()
            .snapshotVersion();

        MarketQuoteResponseDTO response = marketQuoteService.quote(
            authentication(),
            new MarketQuoteRequestDTO(
                "wheat",
                MarketSide.BUY,
                10L,
                snapshotVersion,
                null
            ),
            null
        );

        ArgumentCaptor<MarketQuoteStore.StoredQuote> quoteCaptor =
            ArgumentCaptor.forClass(MarketQuoteStore.StoredQuote.class);
        verify(quoteStore).put(quoteCaptor.capture());
        MarketQuoteStore.StoredQuote storedQuote = quoteCaptor.getValue();

        assertEquals("wheat", response.itemId());
        assertEquals(MarketSide.BUY, response.side());
        assertEquals(10L, response.quantity());
        assertEquals("5", response.unitPrice());
        assertEquals("50", response.totalPrice());
        assertEquals("coins", response.currency());
        assertEquals(snapshotVersion, response.snapshotVersion());
        assertFalse(response.blocked());
        assertTrue(response.operating());
        assertNotNull(response.quoteToken());

        assertEquals(response.quoteToken(), storedQuote.quoteToken());
        assertEquals(playerUuid(), storedQuote.playerUuid());
        assertEquals(response.itemId(), storedQuote.itemId());
        assertEquals(response.side(), storedQuote.side());
        assertEquals(response.quantity(), storedQuote.quantity());
        assertEquals(5L, storedQuote.unitPrice());
        assertEquals(50L, storedQuote.totalPrice());
        assertEquals(response.snapshotVersion(), storedQuote.snapshotVersion());
        assertEquals(response.expiresAt(), storedQuote.expiresAt());
    }

    private Clock fixedClock() {
        return Clock.fixed(
            Instant.parse("2026-04-12T18:30:00Z"),
            ZoneOffset.UTC
        );
    }

    private JwtAuthenticationToken authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject(playerUuid().toString())
            .claim("scope", "api:write")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
        return new JwtAuthenticationToken(jwt);
    }

    private UUID playerUuid() {
        return UUID.fromString("110e8400-e29b-41d4-a716-446655440000");
    }

    private MarketItem marketItem(long baseUnitPrice) {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setBuyUnitEstimate(baseUnitPrice);
        item.setSellUnitEstimate(baseUnitPrice);
        item.setCurrency("coins");
        item.setBaseUnitPrice(baseUnitPrice);
        item.setMinUnitPrice(Math.max(1L, Math.round(baseUnitPrice * 0.5D)));
        item.setMaxUnitPrice(Math.round(baseUnitPrice * 3.0D));
        item.setSegmentSize(50L);
        item.setPriceSensitivity(new BigDecimal("0.0800"));
        item.setBaseRegenQuantity(1L);
        item.setRegenIntervalSeconds(60L);
        item.setCurrentStock(0L);
        item.setMarketMomentum(0L);
        item.setNetPosition(0L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }
}
