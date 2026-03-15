package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.BalanceMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.service.BalanceService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    @Mock
    private BalanceService service;

    @Mock
    private BalanceMapper mapper;

    @InjectMocks
    private BalanceController controller;

    @Test
    void getBalanceByUuid_returnsOk() {
        UUID uuid = UUID.randomUUID();
        Balance balance = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(service.getBalance(uuid)).thenReturn(balance);
        when(mapper.toDto(balance)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.getBalanceByUuid(
            uuid
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service).getBalance(uuid);
        verify(mapper).toDto(balance);
    }

    @Test
    void createBalance_returnsCreatedWithLocationAndBody() {
        UUID uuid = UUID.randomUUID();
        BalanceRequestDTO request = mock(BalanceRequestDTO.class);

        Balance created = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.uuid()).thenReturn(uuid);
        when(service.createBalance(uuid)).thenReturn(created);
        when(created.getUuid()).thenReturn(uuid);
        when(mapper.toDto(created)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.createBalance(
            request
        );

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        assertEquals(
            URI.create("/api/balances/" + uuid),
            resp.getHeaders().getLocation()
        );

        verify(service).createBalance(uuid);
        verify(mapper).toDto(created);
    }

    @Test
    void setBalance_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 500;

        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(service.setBalance(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.setBalance(
            uuid,
            amount
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service).setBalance(uuid, amount);
        verify(mapper).toDto(updated);
    }

    @Test
    void deposit_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 300;

        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(service.deposit(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.deposit(
            uuid,
            amount
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service).deposit(uuid, amount);
        verify(mapper).toDto(updated);
    }

    @Test
    void withdraw_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 200;

        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(service.withdraw(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.withdraw(
            uuid,
            amount
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service).withdraw(uuid, amount);
        verify(mapper).toDto(updated);
    }

    @Test
    void getTopBalances_returnsListOfDtos() {
        int limit = 5;

        Balance b1 = new Balance();
        b1.setUuid(UUID.randomUUID());
        b1.setAmount(1000L);

        Balance b2 = new Balance();
        b2.setUuid(UUID.randomUUID());
        b2.setAmount(800L);

        BalanceResponseDTO d1 = new BalanceResponseDTO(
            b1.getUuid(),
            b1.getAmount()
        );
        BalanceResponseDTO d2 = new BalanceResponseDTO(
            b2.getUuid(),
            b2.getAmount()
        );

        when(service.getTopBalances(limit)).thenReturn(List.of(b1, b2));
        when(mapper.toDto(List.of(b1, b2))).thenReturn(List.of(d1, d2));

        ResponseEntity<List<BalanceResponseDTO>> resp =
            controller.getTopBalances(limit);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(2, resp.getBody().size());

        List<BalanceResponseDTO> list = resp.getBody();

        assertEquals(d1.uuid(), list.get(0).uuid());
        assertEquals(d1.amount(), list.get(0).amount());

        assertEquals(d2.uuid(), list.get(1).uuid());
        assertEquals(d2.amount(), list.get(1).amount());

        verify(service).getTopBalances(limit);
        verify(mapper).toDto(List.of(b1, b2));
    }
}
