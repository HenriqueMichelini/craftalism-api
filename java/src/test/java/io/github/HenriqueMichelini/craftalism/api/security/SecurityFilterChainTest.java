// src/test/java/.../api/security/SecurityFilterChainTest.java
package io.github.HenriqueMichelini.craftalism.api.security;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.config.SecurityConfig;
import io.github.HenriqueMichelini.craftalism.api.controller.PlayerController;
import io.github.HenriqueMichelini.craftalism.api.mapper.PlayerMapper;
import io.github.HenriqueMichelini.craftalism.api.service.PlayerService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
@Import(SecurityConfig.class)
class SecurityFilterChainTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PlayerService playerService;

    @MockitoBean
    PlayerMapper playerMapper;

    @Test
    void noToken_canGetPublicReadEndpoint() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(List.of());
        when(playerMapper.toDto(anyList())).thenReturn(List.of());
        mockMvc
            .perform(get("/api/players"))
            .andExpect(status().isOk());
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
