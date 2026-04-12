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
            true,
            60L
        );
    }

    @Test
    void quote_rejectsStaleSnapshotVersion() {
        MarketItem item = marketItem();
        when(marketItemRepository.count()).thenReturn(1L);
        when(marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc())
            .thenReturn(List.of(item));

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketService.quote(
                    authentication(),
                    new MarketQuoteRequestDTO(
                        "wheat",
                        MarketSide.BUY,
                        10L,
                        "market:stale"
                    )
                )
        );

        assertEquals(MarketRejectionCode.STALE_QUOTE, exception.getCode());
    }

    @Test
    void execute_buyUpdatesBalanceAndStock() {
        MarketItem item = marketItem();
        when(marketItemRepository.count()).thenReturn(1L);
        when(marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc())
            .thenReturn(List.of(item));
        MarketQuoteResponseDTO quote = marketService.quote(
            authentication(),
            new MarketQuoteRequestDTO(
                "wheat",
                MarketSide.BUY,
                10L,
                marketService.getSnapshot().snapshotVersion()
            )
        );

        Balance balance = new Balance(playerUuid(), 1_000L);
        when(
            quoteStore.get(eq(quote.quoteToken()))
        ).thenReturn(
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
            new MarketExecuteRequestDTO(
                "wheat",
                MarketSide.BUY,
                10L,
                quote.quoteToken(),
                quote.snapshotVersion()
            )
        );

        assertEquals("SUCCESS", response.status());
        assertEquals(1_810L, item.getCurrentStock());
        assertEquals(950L, balance.getAmount());
        assertNotNull(response.updatedItem());
        verify(quoteStore).put(any(MarketQuoteStore.StoredQuote.class));
        verify(quoteStore).consume(quote.quoteToken());
        verify(balanceRepository).save(balance);
        verify(marketItemRepository).save(item);
    }

    @Test
    void execute_rejectsExpiredQuote() {
        when(marketItemRepository.count()).thenReturn(1L);
        when(marketItemRepository.findAllByOrderByCategoryIdAscDisplayNameAsc())
            .thenReturn(List.of(marketItem()));
        when(quoteStore.get("missing-token")).thenReturn(Optional.empty());

        MarketRejectionException exception = assertThrows(
            MarketRejectionException.class,
            () ->
                marketService.execute(
                    authentication(),
                    new MarketExecuteRequestDTO(
                        "wheat",
                        MarketSide.BUY,
                        10L,
                        "missing-token",
                        "market:any"
                    )
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

    private MarketItem marketItem() {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setBuyUnitEstimate(5L);
        item.setSellUnitEstimate(4L);
        item.setCurrency("coins");
        item.setCurrentStock(1820L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }
}
