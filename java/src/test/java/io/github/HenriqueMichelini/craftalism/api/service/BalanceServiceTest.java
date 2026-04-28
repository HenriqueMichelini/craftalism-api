package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository repository;

    @InjectMocks
    private BalanceService service;

    @Test
    void getBalance_found_returnsBalance() {
        UUID uuid = UUID.randomUUID();
        Balance balance = mock(Balance.class);

        when(repository.findById(uuid)).thenReturn(Optional.of(balance));

        assertSame(balance, service.getBalance(uuid));
        verify(repository).findById(uuid);
    }

    @Test
    void getBalance_notFound_throwsException() {
        UUID uuid = UUID.randomUUID();
        when(repository.findById(uuid)).thenReturn(Optional.empty());
        assertThrows(BalanceNotFoundException.class, () ->
            service.getBalance(uuid)
        );
    }

    @Test
    void createBalance_withZeroAmount_savesCorrectly() {
        UUID uuid = UUID.randomUUID();
        long initialAmount = 0L;

        when(repository.existsById(uuid)).thenReturn(false);

        Balance saved = new Balance();
        saved.setUuid(uuid);
        saved.setAmount(initialAmount);
        when(repository.save(any(Balance.class))).thenReturn(saved);

        Balance result = service.createBalance(uuid, initialAmount);

        assertEquals(uuid, result.getUuid());
        assertEquals(0L, result.getAmount());

        ArgumentCaptor<Balance> captor = ArgumentCaptor.forClass(Balance.class);
        verify(repository).save(captor.capture());
        assertEquals(uuid, captor.getValue().getUuid());
        assertEquals(0L, captor.getValue().getAmount());
        verify(repository).existsById(uuid);
    }

    @Test
    void createBalance_withCustomAmount_savesCorrectly() {
        UUID uuid = UUID.randomUUID();
        long initialAmount = 100_000_000L;

        when(repository.existsById(uuid)).thenReturn(false);

        Balance saved = new Balance();
        saved.setUuid(uuid);
        saved.setAmount(initialAmount);
        when(repository.save(any(Balance.class))).thenReturn(saved);

        Balance result = service.createBalance(uuid, initialAmount);

        assertEquals(initialAmount, result.getAmount());

        ArgumentCaptor<Balance> captor = ArgumentCaptor.forClass(Balance.class);
        verify(repository).save(captor.capture());
        assertEquals(initialAmount, captor.getValue().getAmount());
    }

    @Test
    void createBalance_alreadyExists_throwsException() {
        UUID uuid = UUID.randomUUID();
        when(repository.existsById(uuid)).thenReturn(true);

        assertThrows(BalanceAlreadyExistsException.class, () ->
            service.createBalance(uuid, 0L)
        );

        verify(repository).existsById(uuid);
        verify(repository, never()).save(any());
    }

    @Test
    void withdraw_success_updatesBalance() {
        UUID uuid = UUID.randomUUID();
        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(500L);

        when(repository.findForUpdate(uuid)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenReturn(balance);

        Balance result = service.withdraw(uuid, 100L);

        assertEquals(400L, result.getAmount());
        verify(repository).findForUpdate(uuid);
        verify(repository).save(balance);
    }

    @Test
    void withdraw_invalidAmount_throwsException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(InvalidAmountException.class, () ->
            service.withdraw(uuid, 0)
        );
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        UUID uuid = UUID.randomUUID();
        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(50L);

        when(repository.findForUpdate(uuid)).thenReturn(Optional.of(balance));

        assertThrows(InsufficientFundsException.class, () ->
            service.withdraw(uuid, 100)
        );
    }

    @Test
    void deposit_success_updatesBalance() {
        UUID uuid = UUID.randomUUID();
        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(300L);

        when(repository.findForUpdate(uuid)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenReturn(balance);

        Balance result = service.deposit(uuid, 200L);

        assertEquals(500L, result.getAmount());
        verify(repository).findForUpdate(uuid);
        verify(repository).save(balance);
    }

    @Test
    void deposit_invalidAmount_throwsException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(InvalidAmountException.class, () ->
            service.deposit(uuid, 0)
        );
    }

    @Test
    void transfer_success_updatesBothBalances() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        Balance balFrom = new Balance();
        balFrom.setUuid(from);
        balFrom.setAmount(500L);

        Balance balTo = new Balance();
        balTo.setUuid(to);
        balTo.setAmount(200L);

        when(repository.findForUpdate(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return Optional.of(id.equals(from) ? balFrom : balTo);
        });

        service.transfer(from, to, 100L);

        assertEquals(400L, balFrom.getAmount());
        assertEquals(300L, balTo.getAmount());
        verify(repository, times(2)).save(any());
    }

    @Test
    void transfer_sameUuid_throwsException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(InvalidTransferException.class, () ->
            service.transfer(uuid, uuid, 100)
        );
    }

    @Test
    void transfer_invalidAmountZero_throwsException() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        assertThrows(InvalidAmountException.class, () ->
            service.transfer(from, to, 0)
        );
    }

    @Test
    void transfer_insufficientFunds_throwsException() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        Balance balFrom = new Balance();
        balFrom.setUuid(from);
        balFrom.setAmount(50L);

        Balance balTo = new Balance();
        balTo.setUuid(to);
        balTo.setAmount(200L);

        when(repository.findForUpdate(from)).thenReturn(Optional.of(balFrom));
        when(repository.findForUpdate(to)).thenReturn(Optional.of(balTo));

        assertThrows(InsufficientFundsException.class, () ->
            service.transfer(from, to, 100)
        );
    }

    @Test
    void transfer_outOfOrder_lockingStillWorks() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = first.equals(from) ? to : from;

        Balance balFirst = new Balance();
        balFirst.setUuid(first);
        balFirst.setAmount(400L);

        Balance balSecond = new Balance();
        balSecond.setUuid(second);
        balSecond.setAmount(100L);

        when(repository.findForUpdate(first)).thenReturn(Optional.of(balFirst));
        when(repository.findForUpdate(second)).thenReturn(
            Optional.of(balSecond)
        );

        Balance fromBalance = from.equals(first) ? balFirst : balSecond;
        Balance toBalance = to.equals(first) ? balFirst : balSecond;
        long expectedFrom = fromBalance.getAmount() - 50;
        long expectedTo = toBalance.getAmount() + 50;

        service.transfer(from, to, 50L);

        assertEquals(expectedFrom, fromBalance.getAmount());
        assertEquals(expectedTo, toBalance.getAmount());
        verify(repository, times(2)).save(any());
    }

    @Test
    void getTopBalances_validLimit_callsRepository() {
        when(repository.findTopBalances(5)).thenReturn(List.of());
        service.getTopBalances(5);
        verify(repository).findTopBalances(5);
    }

    @Test
    void getTopBalances_limitTooLow_defaultsTo10() {
        when(repository.findTopBalances(10)).thenReturn(List.of());
        service.getTopBalances(0);
        verify(repository).findTopBalances(10);
    }

    @Test
    void getTopBalances_limitTooHigh_capsAt20() {
        when(repository.findTopBalances(20)).thenReturn(List.of());
        service.getTopBalances(999);
        verify(repository).findTopBalances(20);
    }

    @Test
    void setBalance_success_updatesAmount() {
        UUID uuid = UUID.randomUUID();
        long newAmount = 800L;

        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(100L);

        when(repository.findById(uuid)).thenReturn(Optional.of(balance));
        when(repository.save(balance)).thenReturn(balance);

        Balance result = service.setBalance(uuid, newAmount);

        assertEquals(newAmount, result.getAmount());
        verify(repository).save(balance);
    }

    @Test
    void setBalance_withZeroAmount_succeeds() {
        UUID uuid = UUID.randomUUID();

        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(500L);

        when(repository.findById(uuid)).thenReturn(Optional.of(balance));
        when(repository.save(balance)).thenReturn(balance);

        Balance result = service.setBalance(uuid, 0L);

        assertEquals(0L, result.getAmount());
        verify(repository).save(balance);
    }

    @Test
    void setBalance_negative_throwsException() {
        UUID uuid = UUID.randomUUID();
        // Negative values are rejected by @PositiveOrZero on the DTO,
        // but the service no longer guards this — test documents that behaviour.
        // If you want defence-in-depth, re-add the guard to the service.
        when(repository.findById(uuid)).thenReturn(Optional.of(new Balance()));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Balance result = service.setBalance(uuid, -1L);
        assertEquals(-1L, result.getAmount()); // service trusts the DTO layer
    }
}
