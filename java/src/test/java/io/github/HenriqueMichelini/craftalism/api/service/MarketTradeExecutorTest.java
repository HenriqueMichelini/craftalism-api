package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MarketRejectionException;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.model.MarketSegment;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketTradeExecutorTest {

    private static final UUID PLAYER_UUID = UUID.fromString(
        "110e8400-e29b-41d4-a716-446655440000"
    );

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private MarketItemRepository marketItemRepository;

    private final MarketTradePlanner tradePlanner = new MarketTradePlanner();

    @Test
    void applyTrade_buyDebitsBalanceAndConsumesSegments() {
        MarketItem item = marketItem(segment(0L, 50L, 50L, 5L));
        Balance balance = new Balance(PLAYER_UUID, 1_000L);
        when(balanceRepository.findForUpdate(PLAYER_UUID)).thenReturn(
            Optional.of(balance)
        );
        MarketTradeExecutor executor = executor();

        MarketTradeExecutor.AppliedTrade appliedTrade = executor.applyTrade(
            PLAYER_UUID,
            item,
            quote(MarketSide.BUY, 10L, 5L, 50L),
            "market:snapshot",
            () -> "market:current"
        );

        assertEquals(10L, appliedTrade.executedQuantity());
        assertEquals(10L, item.getNetPosition());
        assertEquals(950L, balance.getAmount());
        assertEquals(40L, item.getCurrentStock());
        assertEquals(40L, item.getSegments().get(0).getRemainingCapacity());
        verify(balanceRepository).save(balance);
        verify(marketItemRepository).save(item);
    }

    @Test
    void applyTrade_sellCreatesBalanceAndRestoresSegments() {
        MarketItem item = marketItem(segment(0L, 50L, 0L, 5L));
        when(balanceRepository.findForUpdate(PLAYER_UUID)).thenReturn(
            Optional.empty()
        );
        MarketTradeExecutor executor = executor();

        MarketTradeExecutor.AppliedTrade appliedTrade = executor.applyTrade(
            PLAYER_UUID,
            item,
            quote(MarketSide.SELL, 10L, 5L, 50L),
            "market:snapshot",
            () -> "market:current"
        );

        ArgumentCaptor<Balance> balanceCaptor = ArgumentCaptor.forClass(
            Balance.class
        );
        verify(balanceRepository).save(balanceCaptor.capture());

        assertEquals(10L, appliedTrade.executedQuantity());
        assertEquals(-10L, item.getNetPosition());
        assertEquals(50L, balanceCaptor.getValue().getAmount());
        assertEquals(10L, item.getCurrentStock());
        assertEquals(10L, item.getSegments().get(0).getRemainingCapacity());
        verify(marketItemRepository).save(item);
    }

    @Test
    void applyTrade_buyInsufficientFundsLeavesPressureUnchanged() {
        MarketItem item = marketItem(segment(0L, 50L, 50L, 5L));
        Balance balance = new Balance(PLAYER_UUID, 49L);
        when(balanceRepository.findForUpdate(PLAYER_UUID)).thenReturn(
            Optional.of(balance)
        );
        MarketTradeExecutor executor = executor();

        assertThrows(
            MarketRejectionException.class,
            () ->
                executor.applyTrade(
                    PLAYER_UUID,
                    item,
                    quote(MarketSide.BUY, 10L, 5L, 50L),
                    "market:snapshot",
                    () -> "market:current"
                )
        );

        assertEquals(0L, item.getNetPosition());
        assertEquals(50L, item.getCurrentStock());
        assertEquals(50L, item.getSegments().get(0).getRemainingCapacity());
        verify(balanceRepository, never()).save(any());
        verify(marketItemRepository, never()).save(any());
    }

    private MarketTradeExecutor executor() {
        return new MarketTradeExecutor(
            balanceRepository,
            marketItemRepository,
            tradePlanner
        );
    }

    private MarketQuoteStore.StoredQuote quote(
        MarketSide side,
        long quantity,
        long unitPrice,
        long totalPrice
    ) {
        return new MarketQuoteStore.StoredQuote(
            "quote-token",
            PLAYER_UUID,
            "wheat",
            side,
            quantity,
            unitPrice,
            totalPrice,
            "market:snapshot",
            Instant.now().plusSeconds(60L),
            MarketQuote.Status.ACTIVE
        );
    }

    private MarketItem marketItem(MarketSegment... segments) {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setCurrency("coins");
        item.setBaseUnitPrice(5L);
        item.setMinUnitPrice(3L);
        item.setMaxUnitPrice(15L);
        item.setSegmentSize(50L);
        item.setPriceSensitivity(new BigDecimal("0.0800"));
        item.setBaseRegenQuantity(1L);
        item.setRegenIntervalSeconds(60L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        for (MarketSegment segment : segments) {
            item.addSegment(segment);
        }
        tradePlanner.recomputeDerivedProjections(item);
        return item;
    }

    private MarketSegment segment(
        long index,
        long maxCapacity,
        long remainingCapacity,
        long unitPrice
    ) {
        MarketSegment segment = new MarketSegment();
        segment.setSegmentIndex(index);
        segment.setMaxCapacity(maxCapacity);
        segment.setRemainingCapacity(remainingCapacity);
        segment.setUnitPrice(unitPrice);
        return segment;
    }
}
