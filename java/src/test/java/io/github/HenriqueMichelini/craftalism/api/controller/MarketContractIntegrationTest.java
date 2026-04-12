package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.security.WithMockJwt;
import io.github.HenriqueMichelini.craftalism.api.service.MarketQuoteStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
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

    private UUID playerUuid;

    @BeforeEach
    void setUp() {
        marketQuoteStore.clear();
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
            .andExpect(jsonPath("$.categories[0].items[0].itemId").value("wheat"));
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
    void quoteAndExecute_buySuccess_updatesBalanceAndStock() throws Exception {
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
            .andExpect(jsonPath("$.updatedItem.currentStock").value(90));

        Balance balance = balanceRepository.findById(playerUuid).orElseThrow();
        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        assertEquals(950L, balance.getAmount());
        assertEquals(90L, item.getCurrentStock());
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
        item.setSellUnitEstimate(4L);
        item.setCurrency("coins");
        item.setCurrentStock(100L);
        item.setVariationPercent(new BigDecimal("2.3"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-04-12T18:29:42Z"));
        return item;
    }
}
