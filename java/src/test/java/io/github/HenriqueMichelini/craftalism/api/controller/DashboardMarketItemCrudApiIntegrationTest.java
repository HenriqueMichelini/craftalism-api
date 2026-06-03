package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketSide;
import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.model.MarketQuote;
import io.github.HenriqueMichelini.craftalism.api.model.MarketTradeHistory;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketQuoteRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketTradeHistoryRepository;
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

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class DashboardMarketItemCrudApiIntegrationTest {

    private static final String BASE_PATH = "/api/dashboard/market/items";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketItemRepository marketItemRepository;

    @Autowired
    private MarketCategoryRepository marketCategoryRepository;

    @Autowired
    private MarketEventInstanceRepository marketEventInstanceRepository;

    @Autowired
    private MarketQuoteRepository marketQuoteRepository;

    @Autowired
    private MarketTradeHistoryRepository marketTradeHistoryRepository;

    @BeforeEach
    void setup() {
        marketEventInstanceRepository.deleteAll();
        marketQuoteRepository.deleteAll();
        marketTradeHistoryRepository.deleteAll();
        marketItemRepository.deleteAll();
        marketCategoryRepository.deleteAll();
        marketCategoryRepository.save(marketCategory("custom", "Custom", 0));
    }

    @Test
    void marketItemCrud_listCreateUpdateDelete() throws Exception {
        MarketItem existingItem = marketItem("existing_item", "Existing Item");
        existingItem.setNetPosition(7L);
        existingItem.setBuyUnitEstimate(100L);
        existingItem.setSellUnitEstimate(100L);
        marketItemRepository.save(existingItem);

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemId").value("existing_item"))
            .andExpect(jsonPath("$[0].buyUnitEstimate").value(100))
            .andExpect(jsonPath("$[0].sellUnitEstimate").value(70))
            .andExpect(jsonPath("$[0].currentStock").value(0));

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("custom_item"))
            )
            .andExpect(status().isCreated())
            .andExpect(
                header().string("Location", BASE_PATH + "/custom_item")
            )
            .andExpect(jsonPath("$.itemId").value("custom_item"))
            .andExpect(jsonPath("$.categoryId").value("custom"))
            .andExpect(jsonPath("$.categoryDisplayName").value("Custom"))
            .andExpect(jsonPath("$.displayName").value("Custom Item"))
            .andExpect(jsonPath("$.baseUnitPrice").value(100))
            .andExpect(jsonPath("$.sellPricePercentage").value(0.7))
            .andExpect(jsonPath("$.buyUnitEstimate").value(100))
            .andExpect(jsonPath("$.sellUnitEstimate").value(70))
            .andExpect(jsonPath("$.lastUpdatedAt").exists());

        mockMvc
            .perform(
                patch(BASE_PATH + "/custom_item")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").value("custom_item"))
            .andExpect(jsonPath("$.categoryId").value("custom"))
            .andExpect(jsonPath("$.displayName").value("Custom Item"))
            .andExpect(jsonPath("$.categoryDisplayName").value("Custom"))
            .andExpect(jsonPath("$.baseUnitPrice").value(200))
            .andExpect(jsonPath("$.sellPricePercentage").value(0.7))
            .andExpect(jsonPath("$.buyUnitEstimate").value(200))
            .andExpect(jsonPath("$.sellUnitEstimate").value(140));

        mockMvc
            .perform(delete(BASE_PATH + "/custom_item"))
            .andExpect(status().isNoContent());
    }

    @Test
    void marketItemCrud_duplicateItemId_returns409ProblemDetail()
        throws Exception {
        marketItemRepository.save(marketItem("custom_item", "Custom Item"));

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("custom_item"))
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Market item already exists for item ID: custom_item"
                )
            );
    }

    @Test
    void marketItemCrud_missingItem_returns404ProblemDetail() throws Exception {
        mockMvc
            .perform(
                patch(BASE_PATH + "/missing_item")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload())
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Market item not found for item ID: missing_item"
                )
            );
    }

    @Test
    void marketItemCrud_validationErrors_return400ProblemDetail()
        throws Exception {
        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(jsonPath("$.errors.itemId").value("Item ID is required"))
            .andExpect(jsonPath("$.errors.currency").value("Currency is required"));

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        createPayload("invalid_item").replace(
                            "\"minUnitPrice\": 50",
                            "\"minUnitPrice\": 250"
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Minimum unit price must be less than or equal to base unit price"
                )
            );

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        createPayload("flat_spread_item")
                            .replace(
                                "\"minUnitPrice\": 50",
                                "\"minUnitPrice\": 100"
                            )
                            .replace(
                                "\"maxUnitPrice\": 300",
                                "\"maxUnitPrice\": 100"
                            )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Minimum and maximum unit prices must allow a buy/sell estimate spread"
                )
            );

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        createPayload("invalid_sell_percentage_item").replace(
                            "\"sellPricePercentage\": 0.7000",
                            "\"sellPricePercentage\": 1.0000"
                        )
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(
                jsonPath("$.errors.sellPricePercentage").value(
                    "Sell price percentage must be less than 1"
                )
            );
    }

    @Test
    void marketItemCrud_defaultCatalogItemDelete_returns409ProblemDetail()
        throws Exception {
        marketItemRepository.save(marketItem("wheat", "Wheat"));

        mockMvc
            .perform(delete(BASE_PATH + "/wheat"))
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.detail").value(
                    "Market item is managed by the default catalog and cannot be deleted: wheat"
                )
            );
    }

    @Test
    void marketItemCrud_activeQuoteReferencedItemDelete_returns409ProblemDetail()
        throws Exception {
        marketItemRepository.save(marketItem("custom_item", "Custom Item"));
        marketQuoteRepository.save(quote("custom_item"));

        mockMvc
            .perform(delete(BASE_PATH + "/custom_item"))
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.detail").value(
                    "Market item is referenced and cannot be deleted: custom_item"
                )
            );
    }

    @Test
    void marketItemCrud_resolvedQuoteReferencedItemDelete_preservesQuote()
        throws Exception {
        marketItemRepository.save(marketItem("custom_item", "Custom Item"));
        MarketQuote quote = quote("custom_item");
        quote.setStatus(MarketQuote.Status.CONSUMED);
        quote.setResolvedAt(Instant.parse("2026-01-01T00:05:00Z"));
        marketQuoteRepository.save(quote);

        mockMvc
            .perform(delete(BASE_PATH + "/custom_item"))
            .andExpect(status().isNoContent());

        assertTrue(marketQuoteRepository.existsById(quote.getQuoteToken()));
    }

    @Test
    void marketItemCrud_tradeHistoryReferencedItemDelete_preservesHistory()
        throws Exception {
        marketItemRepository.save(marketItem("custom_item", "Custom Item"));
        MarketTradeHistory history = marketTradeHistoryRepository.save(
            tradeHistory("custom_item")
        );

        mockMvc
            .perform(delete(BASE_PATH + "/custom_item"))
            .andExpect(status().isNoContent());

        assertTrue(marketTradeHistoryRepository.existsById(history.getId()));
    }

    @Test
    void marketItemCrud_listRefreshesEligibleBalancedDriftAfterReset()
        throws Exception {
        MarketItem item = marketItem("eligible_drift_item", "Eligible Drift Item");
        item.setBaseUnitPrice(100_000L);
        item.setMinUnitPrice(50_000L);
        item.setMaxUnitPrice(300_000L);
        marketItemRepository.save(item);

        mockMvc
            .perform(post("/api/dashboard/market/drift/reset"))
            .andExpect(status().isOk());

        MarketItem resetItem = marketItemRepository
            .findById("eligible_drift_item")
            .orElseThrow();
        long resetRevision = resetItem.getDriftRevision();

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].buyUnitEstimate").value(100_000));

        MarketItem beforeIntervalItem = marketItemRepository
            .findById("eligible_drift_item")
            .orElseThrow();
        assertEquals(resetRevision, beforeIntervalItem.getDriftRevision());

        beforeIntervalItem.setDriftEvaluatedAt(
            beforeIntervalItem.getDriftEvaluatedAt().minusSeconds(3_600L)
        );
        marketItemRepository.save(beforeIntervalItem);

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].buyUnitEstimate").value(not(100_000)));

        MarketItem refreshedItem = marketItemRepository
            .findById("eligible_drift_item")
            .orElseThrow();
        assertEquals(0L, refreshedItem.getNetPosition());
        assertEquals(resetRevision + 1L, refreshedItem.getDriftRevision());
        assertNotEquals(10_000L, refreshedItem.getDriftMultiplierBasisPoints());
        assertNotEquals(100_000L, refreshedItem.getBuyUnitEstimate());
    }

    @Test
    void marketItemCrud_listUsesEventAwarePricingAndClearsCacheBetweenRequests()
        throws Exception {
        marketItemRepository.save(marketItem("eligible_item", "Eligible Item"));
        MarketEventInstance event = marketEventInstanceRepository.save(
            categoryEvent("custom", 12_000)
        );

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].buyUnitEstimate").value(120))
            .andExpect(jsonPath("$[0].sellUnitEstimate").value(84))
            .andExpect(jsonPath("$[0].variationPercent").value(20.0));

        event.setStatus(MarketEventStatus.CANCELLED);
        event.setUpdatedAt(Instant.now());
        marketEventInstanceRepository.save(event);

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].buyUnitEstimate").value(100))
            .andExpect(jsonPath("$[0].sellUnitEstimate").value(70))
            .andExpect(jsonPath("$[0].variationPercent").value(0.0));
    }

    @Test
    void marketItemCrud_listPreservesPricingForEventIneligibleItem()
        throws Exception {
        marketCategoryRepository.save(marketCategory("other", "Other", 1));
        MarketItem item = marketItem("ineligible_item", "Ineligible Item");
        item.setCategoryId("other");
        marketItemRepository.save(item);
        marketEventInstanceRepository.save(categoryEvent("custom", 12_000));

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].buyUnitEstimate").value(100))
            .andExpect(jsonPath("$[0].sellUnitEstimate").value(70))
            .andExpect(jsonPath("$[0].variationPercent").value(0.0));
    }

    @Test
    void marketItemCrud_createAndUpdateResponsesUseEventAwarePricing()
        throws Exception {
        marketEventInstanceRepository.save(categoryEvent("custom", 12_000));

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("event_item"))
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.buyUnitEstimate").value(120))
            .andExpect(jsonPath("$.sellUnitEstimate").value(84))
            .andExpect(jsonPath("$.variationPercent").value(20.0));

        mockMvc
            .perform(
                patch(BASE_PATH + "/event_item")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.buyUnitEstimate").value(240))
            .andExpect(jsonPath("$.sellUnitEstimate").value(168))
            .andExpect(jsonPath("$.variationPercent").value(20.0));
    }

    private static MarketItem marketItem(String itemId, String displayName) {
        MarketItem item = new MarketItem();
        item.setItemId(itemId);
        item.setCategoryId("custom");
        item.setDisplayName(displayName);
        item.setIconKey("STONE");
        item.setCurrency("coins");
        item.setBaseUnitPrice(100L);
        item.setMinUnitPrice(50L);
        item.setMaxUnitPrice(300L);
        item.setSegmentSize(50L);
        item.setPriceSensitivity(new BigDecimal("0.0800"));
        item.setSellPricePercentage(new BigDecimal("0.7000"));
        item.setBaseRegenQuantity(1L);
        item.setRegenIntervalSeconds(60L);
        item.setNetPosition(0L);
        item.setBuyUnitEstimate(100L);
        item.setSellUnitEstimate(95L);
        item.setCurrentStock(0L);
        item.setMarketMomentum(0L);
        item.setVariationPercent(new BigDecimal("0.00"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.now());
        item.setDriftMultiplierBasisPoints(10_000L);
        item.setDriftRevision(0L);
        item.setDriftEvaluatedAt(Instant.now());
        return item;
    }

    private static MarketCategory marketCategory(
        String categoryId,
        String displayName,
        int displayOrder
    ) {
        MarketCategory category = new MarketCategory();
        category.setCategoryId(categoryId);
        category.setDisplayName(displayName);
        category.setIconKey("CHEST");
        category.setDisplayOrder(displayOrder);
        category.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        category.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return category;
    }

    private static MarketEventInstance categoryEvent(
        String categoryId,
        int effectBasisPoints
    ) {
        Instant now = Instant.now();
        MarketEventInstance event = new MarketEventInstance();
        event.setTemplateId("category_event");
        event.setSource(MarketEventSource.ADMIN);
        event.setScope(MarketEventScope.CATEGORY);
        event.setSelectedCategoryId(categoryId);
        event.setEffectBasisPoints(effectBasisPoints);
        event.setEffectVersion(1);
        event.setBlocking(false);
        event.setStartedAt(now.minusSeconds(60L));
        event.setEndsAt(now.plusSeconds(600L));
        event.setStatus(MarketEventStatus.ACTIVE);
        event.setCreatedAt(now);
        event.setUpdatedAt(now);
        return event;
    }

    private static MarketQuote quote(String itemId) {
        MarketQuote quote = new MarketQuote();
        quote.setQuoteToken(UUID.randomUUID().toString());
        quote.setPlayerUuid(UUID.randomUUID());
        quote.setItemId(itemId);
        quote.setSide(MarketSide.BUY);
        quote.setQuantity(1L);
        quote.setUnitPrice(100L);
        quote.setTotalPrice(100L);
        quote.setSnapshotVersion("snapshot");
        quote.setExpiresAt(Instant.parse("2026-01-01T00:10:00Z"));
        quote.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        quote.setStatus(MarketQuote.Status.ACTIVE);
        return quote;
    }

    private static MarketTradeHistory tradeHistory(String itemId) {
        MarketTradeHistory history = new MarketTradeHistory();
        history.setPlayerUuid(UUID.randomUUID());
        history.setItemId(itemId);
        history.setSide(MarketSide.BUY);
        history.setQuantity(1L);
        history.setUnitPrice(100L);
        history.setTotalPrice(100L);
        history.setCurrency("coins");
        history.setSnapshotVersion("snapshot");
        history.setExecutedAt(Instant.parse("2026-01-01T00:05:00Z"));
        return history;
    }

    private static String createPayload(String itemId) {
        return """
            {
              "itemId": "%s",
              "categoryId": "custom",
              "displayName": "Custom Item",
              "iconKey": "STONE",
              "currency": "coins",
              "baseUnitPrice": 100,
              "minUnitPrice": 50,
              "maxUnitPrice": 300,
              "segmentSize": 50,
              "priceSensitivity": 0.0800,
              "sellPricePercentage": 0.7000,
              "baseRegenQuantity": 1,
              "regenIntervalSeconds": 60,
              "netPosition": 0,
              "minNetPosition": null,
              "maxNetPosition": null,
              "blocked": false,
              "operating": true
            }
            """.formatted(itemId);
    }

    private static String updatePayload() {
        return """
            {
              "displayName": "Should Be Ignored",
              "iconKey": "DIAMOND",
              "currency": "coins",
              "baseUnitPrice": 200,
              "minUnitPrice": 100,
              "maxUnitPrice": 600,
              "segmentSize": 50,
              "priceSensitivity": 0.0800,
              "sellPricePercentage": 0.7000,
              "baseRegenQuantity": 2,
              "regenIntervalSeconds": 120,
              "netPosition": 0,
              "minNetPosition": null,
              "maxNetPosition": null,
              "blocked": false,
              "operating": true
            }
            """;
    }
}
