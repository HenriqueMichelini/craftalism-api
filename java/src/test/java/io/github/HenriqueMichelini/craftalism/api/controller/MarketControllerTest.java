package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketExecuteSuccessResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketQuoteResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.MarketSnapshotResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.service.MarketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock
    private MarketService marketService;

    @InjectMocks
    private MarketController controller;

    @Test
    void getSnapshot_returnsOk() {
        MarketSnapshotResponseDTO response = mock(MarketSnapshotResponseDTO.class);
        when(marketService.getSnapshot()).thenReturn(response);

        ResponseEntity<MarketSnapshotResponseDTO> result = controller.getSnapshot();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(marketService).getSnapshot();
    }

    @Test
    void quote_returnsOk() {
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        MarketQuoteRequestDTO request = mock(MarketQuoteRequestDTO.class);
        MarketQuoteResponseDTO response = mock(MarketQuoteResponseDTO.class);
        when(marketService.quote(authentication, request, null)).thenReturn(response);

        ResponseEntity<MarketQuoteResponseDTO> result = controller.quote(authentication, null, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(marketService).quote(authentication, request, null);
    }

    @Test
    void execute_returnsOk() {
        JwtAuthenticationToken authentication = mock(JwtAuthenticationToken.class);
        MarketExecuteRequestDTO request = mock(MarketExecuteRequestDTO.class);
        MarketExecuteSuccessResponseDTO response = mock(MarketExecuteSuccessResponseDTO.class);
        when(marketService.execute(authentication, request, null)).thenReturn(response);

        ResponseEntity<MarketExecuteSuccessResponseDTO> result = controller.execute(authentication, null, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(marketService).execute(authentication, request, null);
    }
}
