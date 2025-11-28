package io.github.HenriqueMichelini.craftalism.api.controller;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.BalanceMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BalanceController.class)
class BalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BalanceService balanceService;

    @MockitoBean
    private BalanceMapper balanceMapper;

    @Test
    void shouldGetTopBalances() throws Exception {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        Balance balance1 = new Balance(uuid1, 1000000L);
        Balance balance2 = new Balance(uuid2, 500000L);

        when(balanceService.getTopBalances(10))
                .thenReturn(Arrays.asList(balance1, balance2));

        when(balanceMapper.toDto(balance1))
                .thenReturn(new BalanceResponseDTO(uuid1, 1000000L));

        when(balanceMapper.toDto(balance2))
                .thenReturn(new BalanceResponseDTO(uuid2, 500000L));

        // Act & Assert
        mockMvc.perform(get("/api/balances/top?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uuid").value(uuid1.toString()))
                .andExpect(jsonPath("$[0].amount").value(1000000))
                .andExpect(jsonPath("$[1].uuid").value(uuid2.toString()))
                .andExpect(jsonPath("$[1].amount").value(500000));
    }

    @Test
    void shouldUseDefaultLimit() throws Exception {
        when(balanceService.getTopBalances(10))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/balances/top"))
                .andExpect(status().isOk());
    }
}