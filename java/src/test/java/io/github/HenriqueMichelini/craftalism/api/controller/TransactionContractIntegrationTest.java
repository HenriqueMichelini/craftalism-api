package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TransactionContractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PlayerRepository playerRepository;

    private UUID senderUuid;
    private UUID receiverUuid;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        playerRepository.deleteAll();

        senderUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        receiverUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        playerRepository.save(new Player(senderUuid, "TransactionSender"));
        playerRepository.save(new Player(receiverUuid, "TransactionReceiver"));
    }

    @Test
    void listTransactions_returnsPopulatedStablePageEnvelope() throws Exception {
        Transaction transaction = transactionRepository.save(
            new Transaction(senderUuid, receiverUuid, 100L)
        );

        mockMvc
            .perform(get("/api/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(transaction.getId()))
            .andExpect(jsonPath("$.content[0].fromPlayerUuid").value(senderUuid.toString()))
            .andExpect(jsonPath("$.content[0].toPlayerUuid").value(receiverUuid.toString()))
            .andExpect(jsonPath("$.content[0].amount").value(100))
            .andExpect(jsonPath("$.pageable.pageNumber").value(0))
            .andExpect(jsonPath("$.pageable.pageSize").value(20))
            .andExpect(jsonPath("$.pageable.sort.empty").value(false))
            .andExpect(jsonPath("$.pageable.sort.sorted").value(true))
            .andExpect(jsonPath("$.pageable.sort.unsorted").value(false))
            .andExpect(jsonPath("$.pageable.offset").value(0))
            .andExpect(jsonPath("$.pageable.paged").value(true))
            .andExpect(jsonPath("$.pageable.unpaged").value(false))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.sort.empty").value(false))
            .andExpect(jsonPath("$.sort.sorted").value(true))
            .andExpect(jsonPath("$.sort.unsorted").value(false))
            .andExpect(jsonPath("$.numberOfElements").value(1))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.empty").value(false));
    }

    @Test
    void listTransactions_returnsEmptyStablePageEnvelope() throws Exception {
        mockMvc
            .perform(get("/api/transactions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalPages").value(0))
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.numberOfElements").value(0))
            .andExpect(jsonPath("$.empty").value(true));
    }
}
