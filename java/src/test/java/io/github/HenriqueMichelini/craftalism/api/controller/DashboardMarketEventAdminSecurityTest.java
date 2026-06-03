package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.config.SecurityConfig;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketDriftAdminService;
import io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
    DashboardMarketEventAdminController.class,
    DashboardMarketEventTemplateController.class,
    DashboardMarketDriftAdminController.class,
})
@Import(SecurityConfig.class)
class DashboardMarketEventAdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketEventAdminService marketEventAdminService;

    @MockitoBean
    private MarketDriftAdminService marketDriftAdminService;

    @MockitoBean
    private io.github.HenriqueMichelini.craftalism.api.market.application.admin.MarketEventTemplateService marketEventTemplateService;

    @Test
    void apiWriteScopeCannotAccessEventAdminMutationRoute() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/events")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api:write")))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void eventAdminScopeCanPassEventAdminMutationSecurityBoundary() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/events")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_market:admin")))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void internalEventAdminReadRouteIsNotPublic() throws Exception {
        mockMvc
            .perform(get("/api/dashboard/market/events"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void marketAdminScopeCanPassInternalEventAdminReadSecurityBoundary() throws Exception {
        mockMvc
            .perform(
                get("/api/dashboard/market/events")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_market:admin")))
            )
            .andExpect(status().isOk());
    }

    @Test
    void apiWriteScopeCannotAccessEventTemplateMutationRoute() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/event-templates")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api:write")))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void apiWriteScopeCannotAccessEventTemplateUpdateRoute() throws Exception {
        mockMvc
            .perform(
                put("/api/dashboard/market/event-templates/crafting_festival")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api:write")))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void marketAdminScopeCanPassEventTemplateUpdateSecurityBoundary()
        throws Exception {
        mockMvc
            .perform(
                put("/api/dashboard/market/event-templates/crafting_festival")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_market:admin")))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void internalEventTemplateReadRouteIsNotPublic() throws Exception {
        mockMvc
            .perform(get("/api/dashboard/market/event-templates"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void apiWriteScopeCannotAccessDriftResetRoute() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/drift/reset")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_api:write")))
            )
            .andExpect(status().isForbidden());
    }

    @Test
    void marketAdminScopeCanPassDriftResetSecurityBoundary() throws Exception {
        mockMvc
            .perform(
                post("/api/dashboard/market/drift/reset")
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_market:admin")))
            )
            .andExpect(status().isOk());
    }
}
