package io.github.HenriqueMichelini.craftalism.api.market.application.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventPricingService;
import io.github.HenriqueMichelini.craftalism.api.market.infrastructure.store.MarketQuoteStore;
import io.github.HenriqueMichelini.craftalism.api.market.domain.rate.MarketRateLimiter;
import io.github.HenriqueMichelini.craftalism.api.market.domain.snapshot.MarketSnapshotProjector;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradePlanner;
import io.github.HenriqueMichelini.craftalism.api.market.domain.trade.MarketTradeRequestPolicy;
import io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotStateLoader;
import io.github.HenriqueMichelini.craftalism.api.market.application.query.MarketSnapshotService;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
            new MarketSnapshotStateLoader(marketItemRepository, tradePlanner),
            new MarketSnapshotProjector(tradePlanner)
        );
        marketQuoteService = new MarketQuoteService(
            marketSnapshotService,
            quoteStore,
            tradePlanner,
            new MarketPlayerResolver("minecraft-server"),
            requestPolicy(marketSnapshotService),
            60L
        );
    }

    @Test
    void quote_storesSameQuoteValuesReturnedToClient() {
        MarketItem item = marketItem(5L);
        item.setNetPosition(12L);
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
        assertEquals(1, storedQuote.pricingContextVersion());
        assertEquals(12L, storedQuote.pressurePosition());
        assertEquals(0L, storedQuote.driftRevision());
        assertEquals(null, storedQuote.namedEventInstanceId());
        assertEquals(null, storedQuote.eventEffectVersion());
        assertEquals(response.expiresAt(), storedQuote.expiresAt());
    }

    @Test
    void quote_storesActiveEventPricingContext() {
        MarketEventInstance event = categoryEvent(12_000);
        MarketTradePlanner eventPlanner = new MarketTradePlanner(
            eventPricingService(event)
        );
        MarketSnapshotService eventSnapshotService = new MarketSnapshotService(
            new MarketSnapshotStateLoader(marketItemRepository, eventPlanner),
            new MarketSnapshotProjector(eventPlanner)
        );
        MarketQuoteService eventQuoteService = new MarketQuoteService(
            eventSnapshotService,
            quoteStore,
            eventPlanner,
            new MarketPlayerResolver("minecraft-server"),
            requestPolicy(eventSnapshotService),
            60L
        );
        MarketItem item = marketItem(100L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(List.of(item));
        String snapshotVersion = eventSnapshotService
            .getSnapshot()
            .snapshotVersion();

        MarketQuoteResponseDTO response = eventQuoteService.quote(
            authentication(),
            new MarketQuoteRequestDTO(
                "wheat",
                MarketSide.BUY,
                1L,
                snapshotVersion,
                null
            ),
            null
        );

        ArgumentCaptor<MarketQuoteStore.StoredQuote> quoteCaptor =
            ArgumentCaptor.forClass(MarketQuoteStore.StoredQuote.class);
        verify(quoteStore).put(quoteCaptor.capture());
        MarketQuoteStore.StoredQuote storedQuote = quoteCaptor.getValue();

        assertEquals("120", response.unitPrice());
        assertEquals("120", response.totalPrice());
        assertEquals(0L, storedQuote.driftRevision());
        assertEquals(event.getId(), storedQuote.namedEventInstanceId());
        assertEquals(event.getEffectVersion(), storedQuote.eventEffectVersion());
    }

    @Test
    void quote_rejectsUnknownItemWithoutStoringQuote() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(List.of(item));
        String snapshotVersion = marketSnapshotService
            .getSnapshot()
            .snapshotVersion();

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketQuoteService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO(
                        "carrot",
                        MarketSide.BUY,
                        10L,
                        snapshotVersion,
                        null
                    ),
                    null
                )
        );

        assertEquals(MarketRejectionCode.UNKNOWN_ITEM, exception.getCode());
        assertEquals(snapshotVersion, exception.getSnapshotVersion());
        verify(quoteStore, never()).put(
            any(MarketQuoteStore.StoredQuote.class)
        );
    }

    @Test
    void quote_rejectsBlockedItemWithoutStoringQuote() {
        MarketItem item = marketItem(5L);
        item.setBlocked(true);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(List.of(item));
        String snapshotVersion = marketSnapshotService
            .getSnapshot()
            .snapshotVersion();

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketQuoteService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO(
                        "wheat",
                        MarketSide.BUY,
                        10L,
                        snapshotVersion,
                        null
                    ),
                    null
                )
        );

        assertEquals(MarketRejectionCode.ITEM_BLOCKED, exception.getCode());
        assertEquals(snapshotVersion, exception.getSnapshotVersion());
        verify(quoteStore, never()).put(
            any(MarketQuoteStore.StoredQuote.class)
        );
    }

    @Test
    void quote_rejectsNonOperatingItemWithoutStoringQuote() {
        MarketItem item = marketItem(5L);
        item.setOperating(false);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(List.of(item));
        String snapshotVersion = marketSnapshotService
            .getSnapshot()
            .snapshotVersion();

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketQuoteService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO(
                        "wheat",
                        MarketSide.BUY,
                        10L,
                        snapshotVersion,
                        null
                    ),
                    null
                )
        );

        assertEquals(
            MarketRejectionCode.ITEM_NOT_OPERATING,
            exception.getCode()
        );
        assertEquals(snapshotVersion, exception.getSnapshotVersion());
        verify(quoteStore, never()).put(
            any(MarketQuoteStore.StoredQuote.class)
        );
    }

    private Clock fixedClock() {
        return Clock.fixed(
            Instant.parse("2026-04-12T18:30:00Z"),
            ZoneOffset.UTC
        );
    }

    private MarketTradeRequestPolicy requestPolicy(
        MarketSnapshotService snapshotService
    ) {
        return new MarketTradeRequestPolicy(
            snapshotService,
            new MarketRateLimiter(0, Duration.ofSeconds(60L), fixedClock()),
            null,
            true
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
        item.setDriftMultiplierBasisPoints(10_000L);
        item.setDriftRevision(0L);
        item.setDriftEvaluatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }

    private MarketEventPricingService eventPricingService(
        MarketEventInstance event
    ) {
        MarketEventInstanceRepository repository = mock(
            MarketEventInstanceRepository.class
        );
        when(repository.findEffectiveActive(any())).thenReturn(
            Optional.of(event)
        );
        return new MarketEventPricingService(
            new MarketEventLifecycleService(repository)
        );
    }

    private MarketEventInstance categoryEvent(int effectBasisPoints) {
        MarketEventInstance event = new MarketEventInstance();
        event.setId(42L);
        event.setTemplateId("farming_bumper_crop");
        event.setSource(MarketEventSource.SCHEDULER);
        event.setScope(MarketEventScope.CATEGORY);
        event.setSelectedCategoryId("farming");
        event.setEffectBasisPoints(effectBasisPoints);
        event.setEffectVersion(3);
        event.setBlocking(false);
        event.setStartedAt(Instant.parse("2026-04-12T18:00:00Z"));
        event.setEndsAt(Instant.parse("2026-04-12T19:00:00Z"));
        event.setStatus(MarketEventStatus.ACTIVE);
        event.setCreatedAt(Instant.parse("2026-04-12T18:00:00Z"));
        event.setUpdatedAt(Instant.parse("2026-04-12T18:00:00Z"));
        return event;
    }
}
