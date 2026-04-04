package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Player;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransferIncidentRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("local")
class BalanceTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransferIncidentRepository incidentRepository;

    @MockitoSpyBean
    private TransactionRepository transactionRepositorySpy;

    private UUID senderId;
    private UUID receiverId;

    @BeforeEach
    void setup() {
        incidentRepository.deleteAll();
        transactionRepository.deleteAll();
        balanceRepository.deleteAll();
        playerRepository.deleteAll();

        senderId = UUID.randomUUID();
        receiverId = UUID.randomUUID();

        playerRepository.save(new Player(senderId, "SenderOne"));
        playerRepository.save(new Player(receiverId, "ReceiverOne"));

        balanceRepository.save(new Balance(senderId, 1000L));
        balanceRepository.save(new Balance(receiverId, 100L));
    }

    @Test
    void transfer_successful() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-success")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 200))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idempotentReplay").value(false))
            .andExpect(jsonPath("$.transaction.id").exists())
            .andExpect(jsonPath("$.transaction.amount").value(200));

        Balance sender = balanceRepository.findById(senderId).orElseThrow();
        Balance receiver = balanceRepository.findById(receiverId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(800L, sender.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(300L, receiver.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(1, transactionRepository.count());
    }

    @Test
    void transfer_insufficientFunds_returns422() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-funds")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 5_000))
            )
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void transfer_senderNotFound_returns404() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-missing-sender")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(UUID.randomUUID(), receiverId, 100))
            )
            .andExpect(status().isNotFound());
    }


    @Test
    void transfer_receiverNotFound_returns404() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-missing-receiver")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, UUID.randomUUID(), 100))
            )
            .andExpect(status().isNotFound());
    }

    @Test
    void transfer_invalidPayload_returns400() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-invalid")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"fromPlayerUuid\":null,\"toPlayerUuid\":null,\"amount\":0}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.craftalism.com/errors/validation"));
    }

    @Test
    void transfer_idempotentRetry_returnsSameTransaction() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-retry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 100))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idempotentReplay").value(false));

        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-retry")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 100))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idempotentReplay").value(true));

        org.junit.jupiter.api.Assertions.assertEquals(1, transactionRepository.count());
        Balance sender = balanceRepository.findById(senderId).orElseThrow();
        Balance receiver = balanceRepository.findById(receiverId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(900L, sender.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(200L, receiver.getAmount());
    }

    @Test
    void transfer_idempotencyConflict_returns409() throws Exception {
        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-conflict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 100))
            )
            .andExpect(status().isOk());

        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-conflict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 200))
            )
            .andExpect(status().isConflict());

        org.junit.jupiter.api.Assertions.assertTrue(incidentRepository.count() >= 1);
    }

    @Test
    void transfer_rollsBackWhenTransactionPersistenceFails() throws Exception {
        doThrow(new RuntimeException("ledger db failure"))
            .when(transactionRepositorySpy)
            .save(any(Transaction.class));

        mockMvc
            .perform(
                post("/api/balances/transfer")
                    .header("Idempotency-Key", "idem-tx-failure")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(senderId, receiverId, 120))
            )
            .andExpect(status().isInternalServerError());

        Balance sender = balanceRepository.findById(senderId).orElseThrow();
        Balance receiver = balanceRepository.findById(receiverId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(1000L, sender.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(100L, receiver.getAmount());
        org.junit.jupiter.api.Assertions.assertEquals(0, transactionRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(1, incidentRepository.count());
    }

    private String payload(UUID from, UUID to, long amount) {
        return """
            {
              "fromPlayerUuid": "%s",
              "toPlayerUuid": "%s",
              "amount": %d
            }
            """.formatted(from, to, amount);
    }
}
