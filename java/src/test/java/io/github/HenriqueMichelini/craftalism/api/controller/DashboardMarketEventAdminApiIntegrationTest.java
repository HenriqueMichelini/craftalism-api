package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.model.MarketItem;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DashboardMarketEventAdminApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarketEventInstanceRepository eventRepository;

    @Autowired
    private MarketEventTemplateRepository templateRepository;

    @Autowired
    private MarketItemRepository marketItemRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        templateRepository.deleteAll();
        marketItemRepository.deleteAll();
        templateRepository.save(blockingTemplate());
    }

    @Test
    void adminCanStartListUpdateCancelAndSupersedeEvents() throws Exception {
        MvcResult createResult =
            mockMvc
                .perform(
                    post("/api/dashboard/market/events")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "templateId": "rare_customs_hold",
                              "scope": "ITEM",
                              "selectedItemIds": "wheat",
                              "effectBasisPoints": 10000,
                              "blocking": true,
                              "durationSeconds": 900,
                              "reason": "manual test"
                            }
                            """
                        )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("ADMIN"))
                .andExpect(jsonPath("$.actor").value("admin-user"))
                .andExpect(jsonPath("$.auditMetadata", containsString("manual test")))
                .andReturn();
        long eventId = Long.parseLong(
            jsonNumber(createResult.getResponse().getContentAsString(), "id")
        );

        mockMvc
            .perform(get("/api/dashboard/market/events").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].auditMetadata", containsString("manual test")));

        mockMvc
            .perform(
                patch("/api/dashboard/market/events/{id}", eventId)
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "effectBasisPoints": 9500,
                          "blocking": false,
                          "durationSeconds": 1200,
                          "reason": "soften"
                        }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.effectBasisPoints").value(9500))
            .andExpect(jsonPath("$.effectVersion").value(2))
            .andExpect(jsonPath("$.auditMetadata", containsString("before")));

        mockMvc
            .perform(
                post("/api/dashboard/market/events/{id}/cancel", eventId)
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"done\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.endReason").value("CANCELLED"));

        mockMvc
            .perform(
                post("/api/dashboard/market/events/supersede")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateId": "rare_customs_hold",
                          "scope": "ITEM",
                          "selectedItemIds": "carrot",
                          "effectBasisPoints": 10000,
                          "blocking": true,
                          "durationSeconds": 900,
                          "reason": "replacement"
                        }
                        """
                    )
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.selectedItemIds").value("carrot"));
    }

    @Test
    void unknownTemplateReturnsValidationProblemWithoutPersistingEvent()
        throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/events")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateId": "does_not_exist",
                          "scope": "ITEM",
                          "selectedItemIds": "wheat",
                          "durationSeconds": 900,
                          "reason": "invalid template"
                        }
                        """
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
                    "Market event template does not exist."
                )
            )
            .andExpect(jsonPath("$.path").value("/api/dashboard/market/events"))
            .andExpect(jsonPath("$.timestamp").exists());

        assertEquals(0L, eventRepository.count());
    }

    @Test
    void unknownSupersedeTemplateDoesNotEndActiveEvent() throws Exception {
        MvcResult createResult =
            mockMvc
                .perform(
                    post("/api/dashboard/market/events")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """
                            {
                              "templateId": "rare_customs_hold",
                              "scope": "ITEM",
                              "selectedItemIds": "wheat",
                              "durationSeconds": 900,
                              "reason": "active event"
                            }
                            """
                        )
                )
                .andExpect(status().isCreated())
                .andReturn();
        long eventId = Long.parseLong(
            jsonNumber(createResult.getResponse().getContentAsString(), "id")
        );

        mockMvc
            .perform(
                post("/api/dashboard/market/events/supersede")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "templateId": "does_not_exist",
                          "scope": "ITEM",
                          "selectedItemIds": "carrot",
                          "durationSeconds": 900,
                          "reason": "invalid replacement"
                        }
                        """
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            );

        assertEquals(1L, eventRepository.count());
        assertEquals(
            MarketEventStatus.ACTIVE,
            eventRepository.findById(eventId).orElseThrow().getStatus()
        );
    }

    @Test
    void adminCanResetPersistedDriftAndSnapshotVisibleDerivedFields()
        throws Exception {
        marketItemRepository.save(driftedItem());
        String beforeSnapshotVersion = snapshotVersion();

        mockMvc
            .perform(post("/api/dashboard/market/drift/reset").with(adminJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resetItemCount").value(1))
            .andExpect(jsonPath("$.driftMultiplierBasisPoints").value(10_000))
            .andExpect(jsonPath("$.driftEvaluatedAt").exists());

        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        assertEquals(10_000L, item.getDriftMultiplierBasisPoints());
        assertEquals(8L, item.getDriftRevision());
        assertEquals(100L, item.getBuyUnitEstimate());
        assertEquals(70L, item.getSellUnitEstimate());
        assertEquals(
            0,
            BigDecimal.ZERO.compareTo(item.getVariationPercent())
        );

        mockMvc
            .perform(get("/api/market/snapshot"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshotVersion").value(not(beforeSnapshotVersion)))
            .andExpect(jsonPath("$.categories[0].items[0].buyUnitEstimate").value("100"))
            .andExpect(jsonPath("$.categories[0].items[0].sellUnitEstimate").value("70"))
            .andExpect(jsonPath("$.categories[0].items[0].variationPercent").value("0"));
    }

    @Test
    void nonAdminCannotResetPersistedDrift() throws Exception {
        marketItemRepository.save(driftedItem());

        mockMvc
            .perform(
                post("/api/dashboard/market/drift/reset")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api:write")))
            )
            .andExpect(status().isForbidden());

        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        assertNotEquals(10_000L, item.getDriftMultiplierBasisPoints());
    }

    @Test
    void driftResetPersistsEventAwareProjectionsAndClearsCacheBetweenOperations()
        throws Exception {
        marketItemRepository.save(driftedItem());

        mockMvc
            .perform(post("/api/dashboard/market/drift/reset").with(adminJwt()))
            .andExpect(status().isOk());

        eventRepository.save(categoryEvent("farming", 12_000));

        mockMvc
            .perform(post("/api/dashboard/market/drift/reset").with(adminJwt()))
            .andExpect(status().isOk());

        MarketItem item = marketItemRepository.findById("wheat").orElseThrow();
        assertEquals(10_000L, item.getDriftMultiplierBasisPoints());
        assertEquals(9L, item.getDriftRevision());
        assertEquals(120L, item.getBuyUnitEstimate());
        assertEquals(84L, item.getSellUnitEstimate());
        assertEquals(
            0,
            new BigDecimal("20.00").compareTo(item.getVariationPercent())
        );
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt()
            .jwt(jwt -> jwt.subject("admin-user"))
            .authorities(new SimpleGrantedAuthority("SCOPE_market:admin"));
    }

    private String jsonNumber(String body, String field) {
        String needle = "\"" + field + "\":";
        int start = body.indexOf(needle);
        int valueStart = start + needle.length();
        int valueEnd = valueStart;
        while (
            valueEnd < body.length() &&
            Character.isDigit(body.charAt(valueEnd))
        ) {
            valueEnd++;
        }
        return body.substring(valueStart, valueEnd);
    }

    private String snapshotVersion() throws Exception {
        MvcResult result = mockMvc
            .perform(get("/api/market/snapshot"))
            .andExpect(status().isOk())
            .andReturn();
        return jsonString(result.getResponse().getContentAsString(), "snapshotVersion");
    }

    private String jsonString(String body, String field) {
        String needle = "\"" + field + "\":\"";
        int start = body.indexOf(needle);
        int valueStart = start + needle.length();
        int valueEnd = body.indexOf('"', valueStart);
        return body.substring(valueStart, valueEnd);
    }

    private MarketItem driftedItem() {
        MarketItem item = new MarketItem();
        item.setItemId("wheat");
        item.setCategoryId("farming");
        item.setCategoryDisplayName("Farming");
        item.setDisplayName("Wheat");
        item.setIconKey("WHEAT");
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
        item.setBuyUnitEstimate(106L);
        item.setSellUnitEstimate(74L);
        item.setCurrentStock(0L);
        item.setMarketMomentum(0L);
        item.setVariationPercent(new BigDecimal("6.00"));
        item.setBlocked(false);
        item.setOperating(true);
        item.setLastUpdatedAt(Instant.now());
        item.setDriftMultiplierBasisPoints(10_600L);
        item.setDriftRevision(7L);
        item.setDriftEvaluatedAt(Instant.now());
        return item;
    }

    private MarketEventTemplate blockingTemplate() {
        MarketEventTemplate template = new MarketEventTemplate();
        template.setTemplateId("rare_customs_hold");
        template.setRarity(MarketEventRarity.RARE);
        template.setScope(MarketEventScope.ITEM);
        template.setAutomaticWeight(0);
        template.setAutomaticEnabled(false);
        template.setBlockingAllowed(true);
        template.setMinDurationSeconds(900L);
        template.setMaxDurationSeconds(1_800L);
        template.setMinEffectBasisPoints(10_000);
        template.setMaxEffectBasisPoints(10_000);
        template.setEffectDirection("BLOCK");
        template.setCooldownSeconds(21_600L);
        template.setPlayerFacingName("Customs Hold");
        template.setPlayerFacingDescription("A specific good is temporarily held.");
        template.setBroadScopeHint("One item");
        template.setEligibleTargetMetadata("{\"manualOnly\":true}");
        template.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        template.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return template;
    }

    private MarketEventInstance categoryEvent(
        String categoryId,
        int effectBasisPoints
    ) {
        Instant now = Instant.now();
        MarketEventInstance event = new MarketEventInstance();
        event.setTemplateId("category_event");
        event.setSource(MarketEventSource.ADMIN);
        event.setRarity(MarketEventRarity.MEDIUM);
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
}
