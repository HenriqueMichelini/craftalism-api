package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
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

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        templateRepository.deleteAll();
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
}
