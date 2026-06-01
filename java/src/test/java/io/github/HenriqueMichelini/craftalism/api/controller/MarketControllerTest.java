package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketTradeHistoryFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.PageResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import io.github.HenriqueMichelini.craftalism.api.service.MarketTradeHistoryReadService;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock
    private MarketService marketService;

    @Mock
    private MarketTradeHistoryReadService tradeHistoryReadService;

    @InjectMocks
    private MarketController controller;

    @Test
    void getSnapshot_returnsOk() {
        MarketSnapshotResponseDTO response = mock(MarketSnapshotResponseDTO.class);
        when(marketService.getSnapshot()).thenReturn(response);

        ResponseEntity<MarketSnapshotResponseDTO> result = controller.getSnapshot();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(marketService).getSnapshot();
    }

    @Test
    void quote_returnsOk() {
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        MarketQuoteRequestDTO request = mock(MarketQuoteRequestDTO.class);
        MarketQuoteResponseDTO response = mock(MarketQuoteResponseDTO.class);
        when(marketService.quote(authentication, request, null)).thenReturn(response);

        ResponseEntity<MarketQuoteResponseDTO> result = controller.quote(authentication, null, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(marketService).quote(authentication, request, null);
    }

    @Test
    void execute_returnsOk() {
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        MarketExecuteRequestDTO request = mock(MarketExecuteRequestDTO.class);
        MarketExecuteSuccessResponseDTO response = mock(MarketExecuteSuccessResponseDTO.class);
        when(marketService.execute(authentication, request, null)).thenReturn(response);

        ResponseEntity<MarketExecuteSuccessResponseDTO> result = controller.execute(authentication, null, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(marketService).execute(authentication, request, null);
    }

    @Test
    void getTrades_returnsOk() {
        String playerUuid = "110e8400-e29b-41d4-a716-446655440000";
        Instant executedFrom = Instant.parse("2026-05-01T00:00:00Z");
        Instant executedTo = Instant.parse("2026-05-02T00:00:00Z");
        Pageable pageable = PageRequest.of(0, 20);
        Page<MarketTradeHistoryDTO> response = new PageImpl<>(java.util.List.of());
        when(
            tradeHistoryReadService.findTrades(
                new MarketTradeHistoryFilterDTO(
                    playerUuid,
                    "exact",
                    "wheat",
                    "contains",
                    MarketSide.BUY,
                    10L,
                    100L,
                    executedFrom,
                    executedTo
                ),
                pageable
            )
        ).thenReturn(response);

        ResponseEntity<PageResponseDTO<MarketTradeHistoryDTO>> result = controller.getTrades(
            playerUuid,
            "exact",
            "wheat",
            "contains",
            MarketSide.BUY,
            10L,
            100L,
            executedFrom,
            executedTo,
            pageable
        );

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response.getContent(), result.getBody().content());
        assertEquals(response.getTotalElements(), result.getBody().totalElements());
        assertEquals(response.getTotalPages(), result.getBody().totalPages());
        verify(tradeHistoryReadService).findTrades(
            new MarketTradeHistoryFilterDTO(
                playerUuid,
                "exact",
                "wheat",
                "contains",
                MarketSide.BUY,
                10L,
                100L,
                executedFrom,
                executedTo
            ),
            pageable
        );
    }

    @Test
    void getTrade_returnsOk() {
        MarketTradeHistoryDTO response = mock(MarketTradeHistoryDTO.class);
        when(tradeHistoryReadService.getTrade(10L)).thenReturn(response);

        ResponseEntity<MarketTradeHistoryDTO> result = controller.getTrade(10L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(tradeHistoryReadService).getTrade(10L);
    }
}
