package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DashboardMarketEventTemplateApiIntegrationTest {

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
    }

    @Test
    void adminCanCreateAndListEventTemplates() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/event-templates")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validTemplate())
            )
            .andExpect(status().isCreated())
            .andExpect(
                header().string(
                    "Location",
                    "/api/dashboard/market/event-templates/crafting_festival"
                )
            )
            .andExpect(jsonPath("$.templateId").value("crafting_festival"))
            .andExpect(jsonPath("$.effectDirection").value("UP"))
            .andExpect(jsonPath("$.createdAt").exists());

        mockMvc
            .perform(
                get("/api/dashboard/market/event-templates").with(adminJwt())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].templateId").value("crafting_festival"))
            .andExpect(jsonPath("$[0].eligibleTargetMetadata").value("{}"));
    }

    @Test
    void invalidAndDuplicateTemplatesReturnValidationProblems() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/event-templates")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validTemplate())
            )
            .andExpect(status().isCreated());

        mockMvc
            .perform(
                post("/api/dashboard/market/event-templates")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validTemplate())
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.detail").value(
                    "Market event template already exists."
                )
            );

        mockMvc
            .perform(
                post("/api/dashboard/market/event-templates")
                    .with(adminJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        validTemplate()
                            .replace(
                                "\"crafting_festival\"",
                                "\"malformed_metadata\""
                            )
                            .replace("\"{}\"", "\"not-json\"")
                    )
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.detail").value(
                    "Eligible target metadata must be valid JSON."
                )
            );

        assertEquals(1L, templateRepository.count());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt()
            .jwt(jwt -> jwt.subject("admin-user"))
            .authorities(new SimpleGrantedAuthority("SCOPE_market:admin"));
    }

    private String validTemplate() {
        return """
            {
              "templateId": "crafting_festival",
              "rarity": "MEDIUM",
              "scope": "MARKET_WIDE",
              "automaticWeight": 25,
              "automaticEnabled": true,
              "blockingAllowed": false,
              "minDurationSeconds": 1800,
              "maxDurationSeconds": 3600,
              "minEffectBasisPoints": 10200,
              "maxEffectBasisPoints": 10500,
              "effectDirection": "up",
              "cooldownSeconds": 7200,
              "playerFacingName": "Crafting Festival",
              "playerFacingDescription": "Demand is lifting prices across the market.",
              "broadScopeHint": "World market",
              "eligibleTargetMetadata": "{}"
            }
            """;
    }
}
