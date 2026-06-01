// src/test/java/.../api/security/SecurityFilterChainTest.java
package io.github.HenriqueMichelini.craftalism.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.config.SecurityConfig;
import io.github.HenriqueMichelini.craftalism.api.controller.MarketController;
import io.github.HenriqueMichelini.craftalism.api.controller.PlayerController;
import io.github.HenriqueMichelini.craftalism.api.controller.TransactionController;
import io.github.HenriqueMichelini.craftalism.api.mapper.PlayerMapper;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import io.github.HenriqueMichelini.craftalism.api.service.MarketTradeHistoryReadService;
import io.github.HenriqueMichelini.craftalism.api.service.PlayerService;
import io.github.HenriqueMichelini.craftalism.api.service.TransactionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
    PlayerController.class,
    MarketController.class,
    TransactionController.class,
})
@Import(SecurityConfig.class)
class SecurityFilterChainTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlayerService playerService;

    @MockitoBean
    PlayerMapper playerMapper;

    @MockitoBean
    MarketService marketService;

    @MockitoBean
    MarketTradeHistoryReadService marketTradeHistoryReadService;

    @MockitoBean
    TransactionService transactionService;

    @MockitoBean
    TransactionMapper transactionMapper;

    @Test
    void noToken_canGetPublicReadEndpoint() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(List.of());
        when(playerMapper.toDto(anyList())).thenReturn(List.of());
        mockMvc
            .perform(get("/api/players"))
            .andExpect(status().isOk());
    }

    @Test
    void noToken_canGetMarketTradeHistory() throws Exception {
        when(marketTradeHistoryReadService.findTrades(any(), any())).thenReturn(
            Page.empty()
        );
        mockMvc
            .perform(get("/api/market/trades"))
            .andExpect(status().isOk());
    }

    @Test
    void noToken_canGetTransactions() throws Exception {
        when(transactionService.findTransactions(any(), any())).thenReturn(Page.empty());
        mockMvc
            .perform(get("/api/transactions"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockJwt(scopes = { "api:read" })
    void readScope_canGetMarketTradeHistory() throws Exception {
        when(marketTradeHistoryReadService.findTrades(any(), any())).thenReturn(
            Page.empty()
        );
        mockMvc.perform(get("/api/market/trades")).andExpect(status().isOk());
    }

    @Test
    @WithMockJwt(scopes = { "api:read" })
    void readScope_canGet() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(List.of());
        when(playerMapper.toDto(anyList())).thenReturn(List.of());
        mockMvc.perform(get("/api/players")).andExpect(status().isOk());
    }

    @Test
    @WithMockJwt(scopes = { "api:read" }) // write scope missing
    void readOnlyScope_cannotPost() throws Exception {
        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"uuid\":\"00000000-0000-0000-0000-000000000001\",\"name\":\"Test\"}"
                    )
            )
            .andExpect(status().isForbidden());
    }
}
