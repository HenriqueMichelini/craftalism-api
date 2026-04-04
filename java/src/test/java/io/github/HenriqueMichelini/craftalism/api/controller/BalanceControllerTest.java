package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceCreateRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceSetRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceTransferRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.BalanceUpdateRequestDTO;
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
    void createBalance_withZeroAmount_returnsCreated() {
        UUID uuid = UUID.randomUUID();
        long initialAmount = 0L;

        BalanceCreateRequestDTO request = mock(BalanceCreateRequestDTO.class);
        Balance created = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.uuid()).thenReturn(uuid);
        when(request.amount()).thenReturn(initialAmount);
        when(service.createBalance(uuid, initialAmount)).thenReturn(created);
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
        verify(service).createBalance(uuid, initialAmount);
        verify(mapper).toDto(created);
    }

    @Test
    void createBalance_withCustomInitialAmount_returnsCreated() {
        UUID uuid = UUID.randomUUID();
        long initialAmount = 100_000_000L;

        BalanceCreateRequestDTO request = mock(BalanceCreateRequestDTO.class);
        Balance created = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.uuid()).thenReturn(uuid);
        when(request.amount()).thenReturn(initialAmount);
        when(service.createBalance(uuid, initialAmount)).thenReturn(created);
        when(created.getUuid()).thenReturn(uuid);
        when(mapper.toDto(created)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.createBalance(
            request
        );

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(service).createBalance(uuid, initialAmount);
    }

    @Test
    void setBalance_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 500L;

        BalanceSetRequestDTO request = mock(BalanceSetRequestDTO.class);
        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.amount()).thenReturn(amount);
        when(service.setBalance(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.setBalance(
            uuid,
            request
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(service).setBalance(uuid, amount);
        verify(mapper).toDto(updated);
    }

    @Test
    void setBalance_withZeroAmount_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 0L;

        BalanceSetRequestDTO request = mock(BalanceSetRequestDTO.class);
        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.amount()).thenReturn(amount);
        when(service.setBalance(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.setBalance(
            uuid,
            request
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(service).setBalance(uuid, amount);
    }

    @Test
    void deposit_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 300L;

        BalanceUpdateRequestDTO request = mock(BalanceUpdateRequestDTO.class);
        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.amount()).thenReturn(amount);
        when(service.deposit(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.deposit(
            uuid,
            request
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(service).deposit(uuid, amount);
        verify(mapper).toDto(updated);
    }

    @Test
    void withdraw_returnsOk() {
        UUID uuid = UUID.randomUUID();
        long amount = 200L;

        BalanceUpdateRequestDTO request = mock(BalanceUpdateRequestDTO.class);
        Balance updated = mock(Balance.class);
        BalanceResponseDTO dto = mock(BalanceResponseDTO.class);

        when(request.amount()).thenReturn(amount);
        when(service.withdraw(uuid, amount)).thenReturn(updated);
        when(mapper.toDto(updated)).thenReturn(dto);

        ResponseEntity<BalanceResponseDTO> resp = controller.withdraw(
            uuid,
            request
        );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());
        verify(service).withdraw(uuid, amount);
        verify(mapper).toDto(updated);
    }

    @Test
    void transfer_returnsNoContent() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        long amount = 200L;

        BalanceTransferRequestDTO request = mock(
            BalanceTransferRequestDTO.class
        );
        when(request.fromPlayerUuid()).thenReturn(from);
        when(request.toPlayerUuid()).thenReturn(to);
        when(request.amount()).thenReturn(amount);

        ResponseEntity<Void> resp = controller.transfer(request);

        assertEquals(HttpStatus.NO_CONTENT, resp.getStatusCode());
        assertNull(resp.getBody());
        verify(service).transfer(from, to, amount);
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
        assertEquals(d1.uuid(), resp.getBody().get(0).uuid());
        assertEquals(d1.amount(), resp.getBody().get(0).amount());
        assertEquals(d2.uuid(), resp.getBody().get(1).uuid());
        assertEquals(d2.amount(), resp.getBody().get(1).amount());
        verify(service).getTopBalances(limit);
        verify(mapper).toDto(List.of(b1, b2));
    }
}
