package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
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
class DashboardCrudApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @BeforeEach
    void setup() {
        balanceRepository.deleteAll();
        playerRepository.deleteAll();
    }

    @Test
    void playerCrud_createUpdateDelete() throws Exception {
        UUID uuid = UUID.randomUUID();

        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(playerPayload(uuid, "PlayerOne"))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/players/" + uuid))
            .andExpect(jsonPath("$.uuid").value(uuid.toString()))
            .andExpect(jsonPath("$.name").value("PlayerOne"))
            .andExpect(jsonPath("$.createdAt").exists());

        mockMvc
            .perform(
                patch("/api/players/{uuid}", uuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"PlayerTwo\"}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uuid").value(uuid.toString()))
            .andExpect(jsonPath("$.name").value("PlayerTwo"));

        mockMvc
            .perform(delete("/api/players/{uuid}", uuid))
            .andExpect(status().isNoContent());
    }

    @Test
    void playerCrud_duplicateUuid_returns409ProblemDetail() throws Exception {
        UUID uuid = UUID.randomUUID();
        playerRepository.save(new Player(uuid, "ExistingOne"));

        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(playerPayload(uuid, "DifferentOne"))
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Player already exists for UUID: " + uuid
                )
            );
    }

    @Test
    void playerCrud_duplicateName_returns409ProblemDetail() throws Exception {
        playerRepository.save(new Player(UUID.randomUUID(), "ExistingOne"));

        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(playerPayload(UUID.randomUUID(), "ExistingOne"))
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Player already exists for name: ExistingOne"
                )
            );
    }

    @Test
    void playerCrud_validationErrors_return400ProblemDetail() throws Exception {
        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(jsonPath("$.errors.uuid").value("UUID is required"))
            .andExpect(jsonPath("$.errors.name").value("Name is required"));

        mockMvc
            .perform(
                post("/api/players")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"uuid\":\"not-a-uuid\",\"name\":\"PlayerOne\"}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }

    @Test
    void balanceCrud_createPatchDelete() throws Exception {
        UUID uuid = UUID.randomUUID();
        playerRepository.save(new Player(uuid, "PlayerOne"));

        mockMvc
            .perform(
                post("/api/balances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(balancePayload(uuid, 10_000L))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/balances/" + uuid))
            .andExpect(jsonPath("$.uuid").value(uuid.toString()))
            .andExpect(jsonPath("$.amount").value(10_000));

        mockMvc
            .perform(
                patch("/api/balances/{uuid}", uuid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"amount\":25000}")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.uuid").value(uuid.toString()))
            .andExpect(jsonPath("$.amount").value(25_000));

        mockMvc
            .perform(delete("/api/balances/{uuid}", uuid))
            .andExpect(status().isNoContent());
    }

    @Test
    void balanceCrud_unknownPlayer_returns404ProblemDetail() throws Exception {
        UUID uuid = UUID.randomUUID();

        mockMvc
            .perform(
                post("/api/balances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(balancePayload(uuid, 0L))
            )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value("Player not found for UUID: " + uuid)
            );
    }

    @Test
    void balanceCrud_duplicateBalance_returns409ProblemDetail() throws Exception {
        UUID uuid = UUID.randomUUID();
        playerRepository.save(new Player(uuid, "PlayerOne"));
        balanceRepository.save(new Balance(uuid, 0L));

        mockMvc
            .perform(
                post("/api/balances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(balancePayload(uuid, 100L))
            )
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Balance already exists for UUID: " + uuid
                )
            );
    }

    @Test
    void balanceCrud_invalidAmount_returns400ProblemDetail() throws Exception {
        UUID uuid = UUID.randomUUID();
        playerRepository.save(new Player(uuid, "PlayerOne"));

        mockMvc
            .perform(
                post("/api/balances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(balancePayload(uuid, -1L))
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/validation"
                )
            )
            .andExpect(
                jsonPath("$.errors.amount").value(
                    "Amount must be zero or positive"
                )
            );
    }

    @Test
    void playerDelete_referencedByBalance_returns409ProblemDetail()
        throws Exception {
        UUID uuid = UUID.randomUUID();
        playerRepository.save(new Player(uuid, "PlayerOne"));
        balanceRepository.save(new Balance(uuid, 0L));

        mockMvc
            .perform(delete("/api/players/{uuid}", uuid))
            .andExpect(status().isConflict())
            .andExpect(
                jsonPath("$.type").value(
                    "https://api.craftalism.com/errors/business-rule"
                )
            )
            .andExpect(
                jsonPath("$.detail").value(
                    "Player is referenced and cannot be deleted: " + uuid
                )
            );
    }

    private static String playerPayload(UUID uuid, String name) {
        return """
            {"uuid":"%s","name":"%s"}
            """.formatted(uuid, name);
    }

    private static String balancePayload(UUID uuid, long amount) {
        return """
            {"uuid":"%s","amount":%d}
            """.formatted(uuid, amount);
    }
}
