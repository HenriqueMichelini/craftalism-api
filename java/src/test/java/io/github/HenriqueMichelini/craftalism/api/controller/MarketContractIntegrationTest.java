package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.security.WithMockJwt;
import io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
    properties = {
        "craftalism.market.quote-ttl-seconds=1",
        "craftalism.market.quote-rate-limit.max-requests=100",
        "craftalism.market.execute-rate-limit.max-requests=100",
        "craftalism.market.rate-limit.window-seconds=60",
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("local")
class MarketContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private MarketItemRepository marketItemRepository;

    @Autowired
    private MarketQuoteStore marketQuoteStore;

    @Autowired
    private MarketQuoteRepository marketQuoteRepository;

    @Autowired
    private MarketTradeHistoryRepository marketTradeHistoryRepository;

    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        marketQuoteStore.clear();
        marketTradeHistoryRepository.deleteAll();
        balanceRepository.deleteAll();
        playerRepository.deleteAll();
        marketItemRepository.deleteAll();

        playerUuid = UUID.fromString("220e8400-e29b-41d4-a716-446655440000");
        playerRepository.save(new Player(playerUuid, "MarketPlayer"));
        balanceRepository.save(new Balance(playerUuid, 1_000L));
        marketItemRepository.save(wheat());
    }

    @Test
    void snapshot_isPublicAndReturnsOpaqueVersion() throws Exception {
        mockMvc
            .perform(get("/api/market/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshotVersion").value(org.hamcrest.Matchers.startsWith("market:")))
            .andExpect(jsonPath("$.categories[0].categoryId").value("farming"))
            .andExpect(jsonPath("$.categories[0].items[0].itemId").value("wheat"))
            .andExpect(jsonPath("$.categories[0].items[0].marketPressure").value(0))
            .andExpect(jsonPath("$.categories[0].items[0].marketSegment").value(0))
            .andExpect(jsonPath("$.categories[0].items[0].pressureMagnitude").value(0))
            .andExpect(jsonPath("$.categories[0].items[0].currentStock").doesNotExist());
    }

    @Test
    void tradeHistory_requiresReadScope() throws Exception {
        mockMvc
            .perform(get("/api/market/trades"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tradeHistory_listFiltersAndDetailRequireReadScope() throws Exception {
        MarketTradeHistory first = marketTradeHistoryRepository.save(
            tradeHistory(playerUuid, "wheat", MarketSide.BUY, Instant.parse("2026-05-01T10:00:00Z"))
        );
        marketTradeHistoryRepository.save(
            tradeHistory(playerUuid, "wheat", MarketSide.SELL, Instant.parse("2026-05-01T11:00:00Z"))
        );
        marketTradeHistoryRepository.save(
            tradeHistory(UUID.fromString("330e8400-e29b-41d4-a716-446655440000"), "wheat", MarketSide.BUY, Instant.parse("2026-05-01T12:00:00Z"))
        );

        mockMvc
            .perform(
                get("/api/market/trades")
                    .with(playerJwt())
                    .param("playerUuid", playerUuid.toString())
                    .param("itemId", "wheat")
                    .param("side", "BUY")
                    .param("executedFrom", "2026-05-01T10:00:00Z")
                    .param("executedTo", "2026-05-01T10:00:00Z")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(first.getId()))
            .andExpect(jsonPath("$.content[0].playerUuid").value(playerUuid.toString()))
            .andExpect(jsonPath("$.content[0].itemId").value("wheat"))
            .andExpect(jsonPath("$.content[0].side").value("BUY"))
            .andExpect(jsonPath("$.content[0].quantity").value(10))
            .andExpect(jsonPath("$.content[0].unitPrice").value("5"))
            .andExpect(jsonPath("$.content[0].totalPrice").value("50"))
            .andExpect(jsonPath("$.content[0].currency").value("coins"))
            .andExpect(jsonPath("$.content[0].snapshotVersion").value("market:snapshot"));

        mockMvc
            .perform(get("/api/market/trades/{id}", first.getId()).with(playerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(first.getId()))
            .andExpect(jsonPath("$.itemId").value("wheat"));
    }

    @Test
    void quote_requiresWriteScope() throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "snapshotVersion": "%s"
                        }
                        """.formatted(snapshotVersion)
                    )
            )
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void quoteAndExecute_buySuccess_updatesBalanceAndPressure() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteToken").isString())
                .andExpect(jsonPath("$.unitPrice").value("5"))
                .andExpect(jsonPath("$.totalPrice").value("50"))
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.executedQuantity").value(10))
            .andExpect(jsonPath("$.updatedItem.itemId").value("wheat"))
            .andExpect(jsonPath("$.updatedItem.marketPressure").value(10))
            .andExpect(jsonPath("$.updatedItem.marketSegment").value(0))
            .andExpect(jsonPath("$.updatedItem.pressureMagnitude").value(10))
            .andExpect(jsonPath("$.updatedItem.currentStock").doesNotExist());

        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        MarketTradeHistory history = marketTradeHistoryRepository.findAll().get(0);
        assertEquals(950L, balance.getAmount());
        assertEquals(0L, item.getCurrentStock());
        assertEquals(10L, item.getNetPosition());
        assertEquals(MarketQuote.Status.CONSUMED, quote.getStatus());
        assertEquals(1L, marketTradeHistoryRepository.count());
        assertEquals(playerUuid, history.getPlayerUuid());
        assertEquals("wheat", history.getItemId());
        assertEquals("coins", history.getCurrency());
        assertEquals(10L, history.getQuantity());
        assertEquals(5L, history.getUnitPrice());
        assertEquals(50L, history.getTotalPrice());
    }

    @Test
    @WithMockJwt(subject = "minecraft-server")
    void quoteAndExecute_acceptsTrustedMinecraftServerSuppliedPlayerUuid() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s",
                              "playerUuid": "%s"
                            }
                            """.formatted(snapshotVersion, playerUuid)
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteToken").isString())
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );
        MarketQuote persistedQuote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        assertEquals(playerUuid, persistedQuote.getPlayerUuid());

        mockMvc
            .perform(
                post("/api/market/execute")
                    .header("X-Craftalism-Player-Uuid", playerUuid.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCESS"));

        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        MarketQuote consumedQuote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        assertEquals(950L, balance.getAmount());
        assertEquals(10L, item.getNetPosition());
        assertEquals(MarketQuote.Status.CONSUMED, consumedQuote.getStatus());
    }

    @Test
    void quote_acceptsTrustedMinecraftServerHeaderPlayerUuidWithClientIdClaim() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .with(minecraftServerClientJwt())
                        .header("X-Craftalism-Player-Uuid", playerUuid.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteToken").isString())
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        MarketQuote persistedQuote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        assertEquals(playerUuid, persistedQuote.getPlayerUuid());
    }

    @Test
    @WithMockJwt(subject = "minecraft-server")
    void quote_rejectsWhenPlayerContextMissing() throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "snapshotVersion": "%s"
                        }
                        """.formatted(snapshotVersion)
                    )
            )
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("API_UNAVAILABLE"));
    }

    @Test
    @WithMockJwt(subject = "minecraft-server")
    void quote_rejectsMalformedTrustedSuppliedPlayerUuid() throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "snapshotVersion": "%s",
                          "playerUuid": "not-a-uuid"
                        }
                        """.formatted(snapshotVersion)
                    )
            )
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("API_UNAVAILABLE"));
    }

    @Test
    @WithMockJwt(subject = "market-client")
    void quote_rejectsNonTrustedClientSuppliedPlayerUuid() throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "snapshotVersion": "%s",
                          "playerUuid": "%s"
                        }
                        """.formatted(snapshotVersion, playerUuid)
                    )
            )
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("API_UNAVAILABLE"));
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void quote_rejectsStaleSnapshot() throws Exception {
        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "snapshotVersion": "market:stale"
                        }
                        """
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"))
            .andExpect(jsonPath("$.snapshotVersion").value(org.hamcrest.Matchers.startsWith("market:")));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void quote_rejectsInvalidQuantityWithMarketRejection(long quantity) throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": %d,
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quantity, snapshotVersion)
                    )
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"))
            .andExpect(jsonPath("$.message").value("Quantity must be positive."))
            .andExpect(jsonPath("$.snapshotVersion").value(org.hamcrest.Matchers.startsWith("market:")));
    }

    @Test
    void quote_rejectsRateLimitedRequestWithMarketRejection() throws Exception {
        UUID rateLimitedPlayerUuid = UUID.fromString("330e8400-e29b-41d4-a716-446655440000");
        playerRepository.save(new Player(rateLimitedPlayerUuid, "RateLimitedQuotePlayer"));
        balanceRepository.save(new Balance(rateLimitedPlayerUuid, 1_000L));
        String snapshotVersion = snapshotVersion();

        for (int i = 0; i < 100; i++) {
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .with(playerJwt(rateLimitedPlayerUuid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quotePayload(snapshotVersion))
                )
                .andExpect(status().isOk());
        }

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .with(playerJwt(rateLimitedPlayerUuid))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(quotePayload(snapshotVersion))
            )
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.snapshotVersion").value(snapshotVersion));
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_rejectsInsufficientFunds() throws Exception {
        balanceRepository.save(new Balance(playerUuid, 20L));
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        assertEquals(0L, item.getNetPosition());
        assertEquals(20L, balance.getAmount());
        assertEquals(MarketQuote.Status.CONSUMED, quote.getStatus());
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_postConsumeBuyPlanMismatchRejectsStaleQuoteAndConsumesQuote() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        MarketQuote persistedQuote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        persistedQuote.setUnitPrice(persistedQuote.getUnitPrice() + 1L);
        persistedQuote.setTotalPrice(persistedQuote.getTotalPrice() + 10L);
        marketQuoteRepository.save(persistedQuote);

        String executePayload =
            """
            {
              "itemId": "wheat",
              "side": "BUY",
              "quantity": 10,
              "quoteToken": "%s",
              "snapshotVersion": "%s"
            }
            """.formatted(quoteToken, quotedSnapshotVersion);

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"));

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"));

        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        assertEquals(0L, item.getNetPosition());
        assertEquals(1_000L, balance.getAmount());
        assertEquals(MarketQuote.Status.CONSUMED, quote.getStatus());
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_postConsumeSellPlanMismatchRejectsStaleQuoteAndConsumesQuote() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "SELL",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        MarketQuote persistedQuote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        persistedQuote.setUnitPrice(persistedQuote.getUnitPrice() + 1L);
        persistedQuote.setTotalPrice(persistedQuote.getTotalPrice() + 10L);
        marketQuoteRepository.save(persistedQuote);

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "SELL",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"));

        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        assertEquals(0L, item.getNetPosition());
        assertEquals(1_000L, balance.getAmount());
        assertEquals(MarketQuote.Status.CONSUMED, quote.getStatus());
    }

    @ParameterizedTest
    @ValueSource(longs = { 0L, -1L })
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_rejectsInvalidQuantityWithMarketRejection(long quantity) throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": %d,
                          "quoteToken": "unused-token",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quantity, snapshotVersion)
                    )
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("INVALID_QUANTITY"))
            .andExpect(jsonPath("$.message").value("Quantity must be positive."))
            .andExpect(jsonPath("$.snapshotVersion").value(org.hamcrest.Matchers.startsWith("market:")));
    }

    @Test
    void execute_rejectsRateLimitedRequestWithMarketRejection() throws Exception {
        UUID rateLimitedPlayerUuid = UUID.fromString("440e8400-e29b-41d4-a716-446655440000");
        String snapshotVersion = snapshotVersion();

        for (int i = 0; i < 100; i++) {
            mockMvc
                .perform(
                    post("/api/market/execute")
                        .with(playerJwt(rateLimitedPlayerUuid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executePayload("missing-token", snapshotVersion))
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"));
        }

        mockMvc
            .perform(
                post("/api/market/execute")
                    .with(playerJwt(rateLimitedPlayerUuid))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload("missing-token", snapshotVersion))
            )
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.snapshotVersion").value(snapshotVersion));
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_rejectsExpiredQuoteAndRemovesIt() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "quoteToken"
        );
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        MarketQuote persistedQuote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        persistedQuote.setExpiresAt(Instant.now().minusSeconds(5));
        marketQuoteRepository.save(persistedQuote);

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"));

        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        assertTrue(quote.getExpiresAt().isBefore(Instant.now()));
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_rejectsStaleQuoteAfterMarketStateChanges() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "quoteToken"
        );
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        MarketItem item = marketItemRepository.findByItemId("wheat").orElseThrow();
        item.setNetPosition(1L);
        item.setLastUpdatedAt(Instant.now().plusSeconds(5));
        marketItemRepository.save(item);

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"));

        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        MarketItem persistedItem = marketItemRepository.findById("wheat").orElseThrow();
        assertEquals(1L, persistedItem.getNetPosition());
        assertEquals(MarketQuote.Status.INVALIDATED, quote.getStatus());
        assertEquals(0L, marketTradeHistoryRepository.count());
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void execute_replayRejectsConsumedQuote() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "quoteToken"
        );
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        String executePayload =
            """
            {
              "itemId": "wheat",
              "side": "BUY",
              "quantity": 10,
              "quoteToken": "%s",
              "snapshotVersion": "%s"
            }
            """.formatted(quoteToken, quotedSnapshotVersion);

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload)
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(executePayload)
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"));

        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        assertEquals(10L, item.getNetPosition());
        assertEquals(MarketQuote.Status.CONSUMED, quote.getStatus());
        assertEquals(1L, marketTradeHistoryRepository.count());
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void quoteAndExecute_sellSuccess_decreasesPressureAndCreditsBalance() throws Exception {
        MarketItem item = marketItemRepository.findByItemId("wheat").orElseThrow();
        item.setLastUpdatedAt(Instant.now());
        marketItemRepository.save(item);

        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "SELL",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unitPrice").value("5"))
                .andExpect(jsonPath("$.totalPrice").value("50"))
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );

        mockMvc
            .perform(
                post("/api/market/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "SELL",
                          "quantity": 10,
                          "quoteToken": "%s",
                          "snapshotVersion": "%s"
                        }
                        """.formatted(quoteToken, quotedSnapshotVersion)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.executedQuantity").value(10))
            .andExpect(jsonPath("$.updatedItem.marketPressure").value(-10))
            .andExpect(jsonPath("$.updatedItem.marketSegment").value(-1))
            .andExpect(jsonPath("$.updatedItem.pressureMagnitude").value(10))
            .andExpect(jsonPath("$.updatedItem.currentStock").doesNotExist());

        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        MarketItem updatedItem = marketItemRepository.findByItemId("wheat").orElseThrow();
        MarketTradeHistory history = marketTradeHistoryRepository.findAll().get(0);
        assertEquals(1_050L, balance.getAmount());
        assertEquals(-10L, updatedItem.getNetPosition());
        assertEquals(0L, updatedItem.getCurrentStock());
        assertEquals(1L, marketTradeHistoryRepository.count());
        assertEquals(playerUuid, history.getPlayerUuid());
        assertEquals("wheat", history.getItemId());
        assertEquals(10L, history.getQuantity());
        assertEquals(5L, history.getUnitPrice());
        assertEquals(50L, history.getTotalPrice());
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void quote_allowsSellPastEquilibriumWhenNoPressureBoundExists() throws Exception {
        MarketItem item = marketItemRepository.findByItemId("wheat").orElseThrow();
        item.setLastUpdatedAt(Instant.now());
        marketItemRepository.save(item);

        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "SELL",
                          "quantity": 11,
                          "snapshotVersion": "%s"
                        }
                        """.formatted(snapshotVersion)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.unitPrice").value("5"))
            .andExpect(jsonPath("$.totalPrice").value("55"));
    }

    @Test
    @WithMockJwt(playerUuid = "220e8400-e29b-41d4-a716-446655440000")
    void quote_rejectsSellPastMinimumPressureBoundWithInsufficientStock() throws Exception {
        MarketItem item = marketItemRepository.findByItemId("wheat").orElseThrow();
        item.setMinNetPosition(0L);
        item.setLastUpdatedAt(Instant.now());
        marketItemRepository.save(item);

        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "SELL",
                          "quantity": 1,
                          "snapshotVersion": "%s"
                        }
                        """.formatted(snapshotVersion)
                    )
            )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void snapshot_regenerationRecoversPressureWithoutReadingSegments() throws Exception {
        MarketItem item = marketItemRepository.findByItemId("wheat").orElseThrow();
        item.setNetPosition(55L);
        item.setLastUpdatedAt(Instant.now().minusSeconds(5 * 60L));
        marketItemRepository.save(item);

        mockMvc
            .perform(get("/api/market/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories[0].items[0].marketPressure").value(50))
            .andExpect(jsonPath("$.categories[0].items[0].marketSegment").value(1))
            .andExpect(jsonPath("$.categories[0].items[0].pressureMagnitude").value(50))
            .andExpect(jsonPath("$.categories[0].items[0].currentStock").doesNotExist());

        MarketItem regenerated = marketItemRepository.findByItemId("wheat").orElseThrow();
        assertEquals(50L, regenerated.getNetPosition());
        assertEquals(0L, regenerated.getCurrentStock());
    }

    @Test
    void execute_concurrentRequestsOnlyConsumeQuoteOnce() throws Exception {
        String snapshotVersion = snapshotVersion();

        MvcResult quoteResult =
            mockMvc
                .perform(
                    post("/api/market/quotes")
                        .with(playerJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "itemId": "wheat",
                              "side": "BUY",
                              "quantity": 10,
                              "snapshotVersion": "%s"
                            }
                            """.formatted(snapshotVersion)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        String quoteToken = jsonField(quoteResult.getResponse().getContentAsString(), "quoteToken");
        String quotedSnapshotVersion = jsonField(
            quoteResult.getResponse().getContentAsString(),
            "snapshotVersion"
        );
        String executePayload =
            """
            {
              "itemId": "wheat",
              "side": "BUY",
              "quantity": 10,
              "quoteToken": "%s",
              "snapshotVersion": "%s"
            }
            """.formatted(quoteToken, quotedSnapshotVersion);

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> executeRequest = () -> {
                startGate.await();
                return mockMvc
                    .perform(
                        post("/api/market/execute")
                            .with(playerJwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(executePayload)
                    )
                    .andReturn();
            };

            List<Future<MvcResult>> futures = new ArrayList<>();
            futures.add(executor.submit(executeRequest));
            futures.add(executor.submit(executeRequest));

            startGate.countDown();

            int successCount = 0;
            int staleQuoteCount = 0;
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get();
                int statusCode = result.getResponse().getStatus();
                if (statusCode == 200) {
                    successCount++;
                } else if (statusCode == 409) {
                    assertEquals("REJECTED", jsonField(result.getResponse().getContentAsString(), "status"));
                    assertEquals("STALE_QUOTE", jsonField(result.getResponse().getContentAsString(), "code"));
                    staleQuoteCount++;
                }
            }

            assertEquals(1, successCount);
            assertEquals(1, staleQuoteCount);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        MarketQuote quote = marketQuoteRepository.findById(quoteToken).orElseThrow();
        assertEquals(950L, balance.getAmount());
        assertEquals(0L, item.getCurrentStock());
        assertEquals(10L, item.getNetPosition());
        assertEquals(MarketQuote.Status.CONSUMED, quote.getStatus());
    }

    @Test
    @WithMockJwt(subject = "220e8400-e29b-41d4-a716-446655440000")
    void quote_acceptsUuidSubjectWhenPlayerUuidClaimMissing() throws Exception {
        String snapshotVersion = snapshotVersion();

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 5,
                          "snapshotVersion": "%s"
                        }
                        """.formatted(snapshotVersion)
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity").value(5));
    }

    private String snapshotVersion() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/market/snapshot")).andReturn();
        return jsonField(result.getResponse().getContentAsString(), "snapshotVersion");
    }

    private RequestPostProcessor playerJwt() {
        return playerJwt(playerUuid);
    }

    private RequestPostProcessor playerJwt(UUID jwtPlayerUuid) {
        return jwt()
            .jwt(jwt -> {
                jwt.subject(jwtPlayerUuid.toString());
                jwt.claim("player_uuid", jwtPlayerUuid.toString());
                jwt.claim("scope", "api:read api:write");
            })
            .authorities(
                new SimpleGrantedAuthority("SCOPE_api:read"),
                new SimpleGrantedAuthority("SCOPE_api:write")
            );
    }

    private String quotePayload(String snapshotVersion) {
        return """
            {
              "itemId": "wheat",
              "side": "BUY",
              "quantity": 10,
              "snapshotVersion": "%s"
            }
            """.formatted(snapshotVersion);
    }

    private String executePayload(String quoteToken, String snapshotVersion) {
        return """
            {
              "itemId": "wheat",
              "side": "BUY",
              "quantity": 10,
              "quoteToken": "%s",
              "snapshotVersion": "%s"
            }
            """.formatted(quoteToken, snapshotVersion);
    }

    private RequestPostProcessor minecraftServerClientJwt() {
        return jwt()
            .jwt(jwt -> {
                jwt.subject("service-account");
                jwt.claim("client_id", "minecraft-server");
                jwt.claim("scope", "api:read api:write");
            })
            .authorities(
                new SimpleGrantedAuthority("SCOPE_api:read"),
                new SimpleGrantedAuthority("SCOPE_api:write")
            );
    }

    private String jsonField(String body, String field) {
        String needle = "\"" + field + "\":\"";
        int start = body.indexOf(needle);
        if (start < 0) {
            throw new IllegalStateException("Missing field " + field + " in " + body);
        }
        int valueStart = start + needle.length();
        int valueEnd = body.indexOf('"', valueStart);
        return body.substring(valueStart, valueEnd);
    }

    private MarketItem wheat() {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
        item.setBuyUnitEstimate(5L);
        item.setSellUnitEstimate(5L);
        item.setCurrency("coins");
        item.setCurrentStock(0L);
        item.setMarketMomentum(0L);
        item.setBaseUnitPrice(5L);
        item.setMinUnitPrice(3L);
        item.setMaxUnitPrice(15L);
        item.setSegmentSize(50L);
        item.setPriceSensitivity(new BigDecimal("0.0800"));
        item.setBaseRegenQuantity(1L);
        item.setRegenIntervalSeconds(60L);
        item.setNetPosition(0L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }

    private MarketTradeHistory tradeHistory(
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
