package io.github.HenriqueMichelini.craftalism.api.exceptions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.controller.PlayerController;
import io.github.HenriqueMichelini.craftalism.api.mapper.PlayerMapper;
import io.github.HenriqueMichelini.craftalism.api.service.PlayerService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PlayerController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private PlayerMapper playerMapper;

    @Test
    void businessException_returnsProblemDetailContract() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(playerService.getPlayerByUuid(uuid)).thenThrow(
            new PlayerNotFoundException(uuid)
        );

        mockMvc
            .perform(get("/api/players/{uuid}", uuid))
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value("Player not found for UUID: " + uuid)
            )
            .andExpect(jsonPath("$.path").value("/api/players/" + uuid))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void beanValidationException_returnsValidationProblemDetailWithFieldErrors()
        throws Exception {
        String invalidBody = "{}";

        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody)
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(jsonPath("$.detail").value("Validation failed"))
            .andExpect(jsonPath("$.errors.uuid").value("UUID is required"))
            .andExpect(jsonPath("$.errors.name").value("Name is required"))
            .andExpect(jsonPath("$.path").value("/api/players"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void unexpectedException_returnsInternalProblemDetailWithoutLeakingDetails()
        throws Exception {
        UUID uuid = UUID.randomUUID();

        when(playerService.getPlayerByUuid(uuid)).thenThrow(
            new RuntimeException("db connection exploded")
        );

        mockMvc
            .perform(get("/api/players/{uuid}", uuid))
            .andExpect(status().isInternalServerError())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/internal"
                )
            )
            .andExpect(
                jsonPath("$.detail").value("Unexpected internal server error")
            )
            .andExpect(jsonPath("$.path").value("/api/players/" + uuid))
            .andExpect(jsonPath("$.timestamp").exists());
    }
}
