package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionCode;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class MarketServiceTest {

    @Mock
    private MarketItemRepository marketItemRepository;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private MarketQuoteRepository marketQuoteRepository;

    @Mock
    private MarketQuoteStore quoteStore;

    private MarketService marketService;

    @BeforeEach
    void setUp() {
        marketService = new MarketService(
            marketItemRepository,
            balanceRepository,
            quoteStore,
            marketQuoteRepository,
            new DefaultMarketCatalog(),
            true,
            60L,
            "minecraft-server"
        );
    }

    @Test
    void quote_rejectsStaleSnapshotVersion() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO("wheat", MarketSide.BUY, 10L, "market:stale", null),
                    null
                )
        );

        assertEquals(MarketRejectionCode.STALE_QUOTE, exception.getCode());
    }

    @Test
    void execute_buyUpdatesBalanceAndPressure() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketQuoteResponseDTO quote = marketService.quote(
            authentication(),
            new MarketQuoteRequestDTO("wheat", MarketSide.BUY, 10L, marketService.getSnapshot().snapshotVersion(), null),
            null
        );

        Balance balance = new Balance(playerUuid(), 1_000L);
        when(quoteStore.get(eq(quote.quoteToken()))).thenReturn(
            Optional.of(
                new MarketQuoteStore.StoredQuote(
                    quote.quoteToken(),
                    playerUuid(),
                    "wheat",
                    MarketSide.BUY,
                    10L,
                    5L,
                    50L,
                    quote.snapshotVersion(),
                    quote.expiresAt(),
                    MarketQuote.Status.ACTIVE
                )
            )
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(Optional.of(item));
        when(balanceRepository.findForUpdate(playerUuid())).thenReturn(Optional.of(balance));
        when(quoteStore.consume(quote.quoteToken())).thenReturn(true);

        MarketExecuteSuccessResponseDTO response = marketService.execute(
            authentication(),
            new MarketExecuteRequestDTO("wheat", MarketSide.BUY, 10L, quote.quoteToken(), quote.snapshotVersion(), null),
            null
        );

        assertEquals("SUCCESS", response.status());
        assertEquals(10L, item.getNetPosition());
        assertEquals(0L, item.getCurrentStock());
        assertEquals(0L, item.getMarketMomentum());
        assertEquals(950L, balance.getAmount());
        assertNotNull(response.updatedItem());
        verify(balanceRepository).save(balance);
        verify(marketItemRepository).save(item);
    }

    @Test
    void quote_buyTraversesVirtualPressureSegmentsProgressively() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketQuoteResponseDTO quote = marketService.quote(
            authentication(),
            new MarketQuoteRequestDTO("wheat", MarketSide.BUY, 60L, marketService.getSnapshot().snapshotVersion(), null),
            null
        );

        assertEquals("6", quote.unitPrice());
        assertEquals("310", quote.totalPrice());
        verify(quoteStore).put(any(MarketQuoteStore.StoredQuote.class));
    }

    @Test
    void execute_buyAcrossVirtualSegments_updatesExecutedQuantityAndDerivedProjections() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));
        String snapshotVersion = marketService.getSnapshot().snapshotVersion();

        Balance balance = new Balance(playerUuid(), 1_000L);
        when(quoteStore.get("segment-quote")).thenReturn(
            Optional.of(
                new MarketQuoteStore.StoredQuote(
                    "segment-quote",
                    playerUuid(),
                    "wheat",
                    MarketSide.BUY,
                    60L,
                    6L,
                    310L,
                    snapshotVersion,
                    Instant.now().plusSeconds(60L),
                    MarketQuote.Status.ACTIVE
                )
            )
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(Optional.of(item));
        when(balanceRepository.findForUpdate(playerUuid())).thenReturn(Optional.of(balance));
        when(quoteStore.consume("segment-quote")).thenReturn(true);

        MarketExecuteSuccessResponseDTO response = marketService.execute(
            authentication(),
            new MarketExecuteRequestDTO("wheat", MarketSide.BUY, 60L, "segment-quote", snapshotVersion, null),
            null
        );

        assertEquals("SUCCESS", response.status());
        assertEquals(60L, response.executedQuantity());
        assertEquals(60L, item.getNetPosition());
        assertEquals(0L, item.getCurrentStock());
        assertEquals(1L, item.getMarketMomentum());
        assertEquals(6L, item.getBuyUnitEstimate());
        assertEquals(6L, item.getSellUnitEstimate());
        assertEquals(690L, balance.getAmount());
    }

    @Test
    void execute_buyAtSegmentBoundary_mutatesPressurePosition() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));
        String snapshotVersion = marketService.getSnapshot().snapshotVersion();

        Balance balance = new Balance(playerUuid(), 1_000L);
        when(quoteStore.get("exhaust-quote")).thenReturn(
            Optional.of(
                new MarketQuoteStore.StoredQuote(
                    "exhaust-quote",
                    playerUuid(),
                    "wheat",
                    MarketSide.BUY,
                    50L,
                    5L,
                    250L,
                    snapshotVersion,
                    Instant.now().plusSeconds(60L),
                    MarketQuote.Status.ACTIVE
                )
            )
        );
        when(marketItemRepository.findForUpdate("wheat")).thenReturn(Optional.of(item));
        when(balanceRepository.findForUpdate(playerUuid())).thenReturn(Optional.of(balance));
        when(quoteStore.consume("exhaust-quote")).thenReturn(true);

        MarketExecuteSuccessResponseDTO response = marketService.execute(
            authentication(),
            new MarketExecuteRequestDTO("wheat", MarketSide.BUY, 50L, "exhaust-quote", snapshotVersion, null),
            null
        );

        assertEquals("SUCCESS", response.status());
        assertEquals(50L, response.executedQuantity());
        assertEquals(50L, item.getNetPosition());
        assertEquals(0L, item.getCurrentStock());
        assertEquals(1L, item.getMarketMomentum());
        assertEquals(750L, balance.getAmount());
    }

    @Test
    void quote_buyAllowsQuantityBeyondLegacyStockWhenNoPressureBoundExists() {
        MarketItem item = marketItem(5L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketQuoteResponseDTO quote = marketService.quote(
            authentication(),
            new MarketQuoteRequestDTO("wheat", MarketSide.BUY, 41L, marketService.getSnapshot().snapshotVersion(), null),
            null
        );

        assertEquals("5", quote.unitPrice());
        assertEquals("205", quote.totalPrice());
        verify(quoteStore).put(any(MarketQuoteStore.StoredQuote.class));
    }

    @Test
    void quote_buyRejectsWhenQuantityExceedsMaximumPressureBound() {
        MarketItem item = marketItem(5L);
        item.setMaxNetPosition(40L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO("wheat", MarketSide.BUY, 41L, marketService.getSnapshot().snapshotVersion(), null),
                    null
                )
        );

        assertEquals(MarketRejectionCode.INSUFFICIENT_STOCK, exception.getCode());
        verify(quoteStore, never()).put(any(MarketQuoteStore.StoredQuote.class));
    }

    @Test
    void quote_sellRejectsWhenQuantityExceedsMinimumPressureBound() {
        MarketItem item = marketItem(5L);
        item.setMinNetPosition(0L);
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO(
                        "wheat",
                        MarketSide.SELL,
                        1L,
                        marketService.getSnapshot().snapshotVersion(),
                        null
                    ),
                    null
                )
        );

        assertEquals(MarketRejectionCode.INSUFFICIENT_STOCK, exception.getCode());
        verify(quoteStore, never()).put(any(MarketQuoteStore.StoredQuote.class));
    }

    @Test
    void quote_buyLargeQuantityIsNotLimitedByLegacyStock() {
        MarketItem item = marketItem(14L);
        item.setItemId("iron_ingot");
        item.setCategoryId("mining");
        item.setCategoryDisplayName("Mining");
        item.setDisplayName("Iron Ingot");
        item.setIconKey("IRON_INGOT");
        item.setVariationPercent(new BigDecimal("1.1"));
        when(marketItemRepository.findAllForMarketRead()).thenReturn(java.util.List.of(item));

        MarketQuoteResponseDTO quote = marketService.quote(
            authentication(),
            new MarketQuoteRequestDTO("iron_ingot", MarketSide.BUY, 2_304L, marketService.getSnapshot().snapshotVersion(), null),
            null
        );

        assertEquals(0L, item.getCurrentStock());
        assertEquals(0L, item.getMarketMomentum());
        verify(quoteStore).put(any(MarketQuoteStore.StoredQuote.class));
    }

    @Test
    void initializeCatalogIfEmpty_seedsCarrotFromDefaultCatalog() {
        when(marketItemRepository.count()).thenReturn(0L);

        marketService.initializeCatalogIfEmpty();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<MarketItem>> itemCaptor = ArgumentCaptor.forClass(
            Iterable.class
        );
        verify(marketItemRepository).saveAll(itemCaptor.capture());

        List<MarketItem> savedItems = new java.util.ArrayList<>();
        itemCaptor.getValue().forEach(savedItems::add);

        MarketItem carrot = savedItems
            .stream()
            .filter(item -> item.getItemId().equals("carrot"))
            .findFirst()
            .orElseThrow();
        assertEquals(0L, carrot.getCurrentStock());
        assertEquals(10_000L, carrot.getBuyUnitEstimate());
        assertEquals(9_616L, carrot.getSellUnitEstimate());
        assertEquals(0, carrot.getSegments().size());
    }

    @Test
    void execute_rejectsExpiredQuote() {
        when(marketItemRepository.findAllForMarketRead())
            .thenReturn(java.util.List.of(marketItem(5L)));
        when(quoteStore.get("missing-token")).thenReturn(Optional.empty());

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketService.execute(
                    authentication(),
                    new MarketExecuteRequestDTO("wheat", MarketSide.BUY, 10L, "missing-token", "market:any", null),
                    null
                )
        );

        assertEquals(MarketRejectionCode.QUOTE_EXPIRED, exception.getCode());
        verify(balanceRepository, never()).save(any());
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
