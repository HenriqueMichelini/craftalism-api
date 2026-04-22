package io.github.HenriqueMichelini.craftalism.api.exceptions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.HenriqueMichelini.craftalism.api.controller.MarketController;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class MarketExceptionHandlerContractTest {

    @Mock
    private MarketService marketService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(new MarketController(marketService))
                .setControllerAdvice(
                    new MarketExceptionHandler(),
                    new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void marketRejection_returnsContractBody() throws Exception {
        when(marketService.quote(isNull(), any(MarketQuoteRequestDTO.class), isNull()))
            .thenThrow(
                new MarketRejectionException(
                    MarketRejectionCode.STALE_QUOTE,
                    "Quote is no longer valid.",
                    HttpStatus.CONFLICT,
                    "market:123"
                )
            );

        mockMvc
            .perform(
                post("/api/market/quotes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "itemId": "wheat",
                          "side": "BUY",
                          "quantity": 32,
                          "snapshotVersion": "market:old"
                        }
                        """
                    )
            )
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.code").value("STALE_QUOTE"))
            .andExpect(jsonPath("$.message").value("Quote is no longer valid."))
            .andExpect(jsonPath("$.snapshotVersion").value("market:123"));
    }
}
