package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.MarketCategory;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketCategoryRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class DashboardMarketCategoryCrudApiIntegrationTest {

    private static final String BASE_PATH = "/api/dashboard/market/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketCategoryRepository marketCategoryRepository;

    @Autowired
    private MarketItemRepository marketItemRepository;

    @BeforeEach
    void setup() {
        marketItemRepository.deleteAll();
        marketCategoryRepository.deleteAll();
    }

    @Test
    void marketCategoryCrud_listCreateUpdateDelete() throws Exception {
        marketCategoryRepository.save(marketCategory("farming", "Farming", 0));

        mockMvc
            .perform(get(BASE_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].categoryId").value("farming"))
            .andExpect(jsonPath("$[0].displayName").value("Farming"))
            .andExpect(jsonPath("$[0].iconKey").value("WHEAT"))
            .andExpect(jsonPath("$[0].displayOrder").value(0));

        mockMvc
            .perform(
                post(BASE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createPayload("custom"))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", BASE_PATH + "/custom"))
            .andExpect(jsonPath("$.categoryId").value("custom"))
            .andExpect(jsonPath("$.displayName").value("Custom"))
            .andExpect(jsonPath("$.iconKey").value("CHEST"))
            .andExpect(jsonPath("$.displayOrder").value(9));

        mockMvc
            .perform(
                patch(BASE_PATH + "/custom")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatePayload())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryId").value("custom"))
            .andExpect(jsonPath("$.displayName").value("Updated Custom"))
            .andExpect(jsonPath("$.iconKey").value("BARREL"))
            .andExpect(jsonPath("$.displayOrder").value(3));

        mockMvc
            .perform(delete(BASE_PATH + "/custom"))
            .andExpect(status().isNoContent());
    }

    @Test
    void marketCategoryCrud_referencedCategoryDelete_returns409ProblemDetail()
        throws Exception {
        marketCategoryRepository.save(marketCategory("custom", "Custom", 0));
        marketItemRepository.save(marketItem("custom"));

        mockMvc
            .perform(delete(BASE_PATH + "/custom"))
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.detail").value(
                    "Market category is referenced and cannot be deleted: custom"
                )
            );
    }

    @Test
    void marketCategoryCrud_deleteFinalCustomItem_allowsCategoryDelete()
        throws Exception {
        marketCategoryRepository.save(marketCategory("custom", "Custom", 0));
        marketItemRepository.save(marketItem("custom_item"));

        mockMvc
            .perform(delete("/api/dashboard/market/items/custom_item"))
            .andExpect(status().isNoContent());

        mockMvc
            .perform(delete(BASE_PATH + "/custom"))
            .andExpect(status().isNoContent());
    }

    private static MarketCategory marketCategory(
        String categoryId,
        String displayName,
        int displayOrder
    ) {
        MarketCategory category = new MarketCategory();
        category.setCategoryId(categoryId);
        category.setDisplayName(displayName);
        category.setIconKey("WHEAT");
        category.setDisplayOrder(displayOrder);
        category.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        category.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return category;
    }

    private static MarketItem marketItem(String itemId) {
        MarketItem item = new MarketItem();
        item.setItemId(itemId);
        item.setCategoryId("custom");
        item.setDisplayName("Custom Item");
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
        item.setSellUnitEstimate(70L);
        item.setCurrentStock(0L);
        item.setMarketMomentum(0L);
        item.setVariationPercent(new BigDecimal("0.00"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        item.setDriftMultiplierBasisPoints(10_000L);
        item.setDriftRevision(0L);
        item.setDriftEvaluatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return item;
    }

    private static String createPayload(String categoryId) {
        return """
            {
              "categoryId": "%s",
              "displayName": "Custom",
              "iconKey": "CHEST",
              "displayOrder": 9
            }
            """.formatted(categoryId);
    }

    private static String updatePayload() {
        return """
            {
              "displayName": "Updated Custom",
              "iconKey": "BARREL",
              "displayOrder": 3
            }
            """;
    }
}
