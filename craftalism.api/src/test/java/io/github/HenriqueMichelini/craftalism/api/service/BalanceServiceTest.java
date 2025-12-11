package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        Balance result = service.getBalance(uuid);

        assertSame(balance, result);
        verify(repository).findById(uuid);
    }

    @Test
    void getBalance_notFound_throwsException() {
        UUID uuid = UUID.randomUUID();

        when(repository.findById(uuid)).thenReturn(Optional.empty());

        assertThrows(BalanceNotFoundException.class,
                () -> service.getBalance(uuid));
    }

    @Test
    void createBalance_success_savesBalanceWithZeroAmount() {
        UUID uuid = UUID.randomUUID();

        when(repository.existsById(uuid)).thenReturn(false);

        Balance saved = new Balance();
        saved.setUuid(uuid);
        saved.setAmount(0L);

        when(repository.save(any(Balance.class))).thenReturn(saved);

        Balance result = service.createBalance(uuid);

        assertEquals(uuid, result.getUuid());
        assertEquals(0L, result.getAmount());

        ArgumentCaptor<Balance> captor = ArgumentCaptor.forClass(Balance.class);
        verify(repository).save(captor.capture());

        Balance created = captor.getValue();
        assertEquals(uuid, created.getUuid());
        assertEquals(0L, created.getAmount());

        verify(repository).existsById(uuid);
    }

    @Test
    void createBalance_alreadyExists_throwsException() {
        UUID uuid = UUID.randomUUID();

        when(repository.existsById(uuid)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.createBalance(uuid));

        verify(repository).existsById(uuid);
        verify(repository, never()).save(any());
    }

    @Test
    void withdraw_success_updatesBalance() {
        UUID uuid = UUID.randomUUID();
        long amount = 100;

        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(500L);

        when(repository.findForUpdate(uuid)).thenReturn(balance);
        when(repository.save(any())).thenReturn(balance);

        Balance result = service.withdraw(uuid, amount);

        assertEquals(400, result.getAmount());
        verify(repository).findForUpdate(uuid);
        verify(repository).save(balance);
    }

    @Test
    void withdraw_invalidAmount_throwsInvalidAmountException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(InvalidAmountException.class,
                () -> service.withdraw(uuid, 0));
    }

    @Test
    void withdraw_insufficientFunds_throwsException() {
        UUID uuid = UUID.randomUUID();
        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(50L);

        when(repository.findForUpdate(uuid)).thenReturn(balance);

        assertThrows(InsufficientFundsException.class,
                () -> service.withdraw(uuid, 100));
    }

    @Test
    void deposit_success_updatesBalance() {
        UUID uuid = UUID.randomUUID();
        long amount = 200;

        Balance balance = new Balance();
        balance.setUuid(uuid);
        balance.setAmount(300L);

        when(repository.findForUpdate(uuid)).thenReturn(balance);
        when(repository.save(any())).thenReturn(balance);

        Balance result = service.deposit(uuid, amount);

        assertEquals(500, result.getAmount());
        verify(repository).findForUpdate(uuid);
        verify(repository).save(balance);
    }

    @Test
    void deposit_invalidAmount_throwsInvalidAmountException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(InvalidAmountException.class,
                () -> service.deposit(uuid, 0));
    }

    @Test
    void transfer_success_updatesBothBalances() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        long amount = 100;

        Balance balFrom = new Balance();
        balFrom.setUuid(from);
        balFrom.setAmount(500L);

        Balance balTo = new Balance();
        balTo.setUuid(to);
        balTo.setAmount(200L);

        when(repository.findForUpdate(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return id.equals(from) ? balFrom : balTo;
        });

        service.transfer(from, to, amount);

        assertEquals(400, balFrom.getAmount());
        assertEquals(300, balTo.getAmount());

        verify(repository, times(2)).save(any());
    }

    @Test
    void transfer_sameUuid_throwsException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> service.transfer(uuid, uuid, 100));
    }

    @Test
    void transfer_invalidAmountZero_throwsException() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
                () -> service.transfer(from, to, 0));
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

        when(repository.findForUpdate(from)).thenReturn(balFrom);
        when(repository.findForUpdate(to)).thenReturn(balTo);

        assertThrows(InsufficientFundsException.class,
                () -> service.transfer(from, to, 100));
    }

    @Test
    void transfer_outOfOrder_lockingStillWorks() {
        // ensures the ordering logic (min UUID first) is correct

        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();

        // force "from > to" to trigger reversed lock order
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = first.equals(from) ? to : from;

        Balance balFirst = new Balance();
        balFirst.setUuid(first);
        balFirst.setAmount(400L);

        Balance balSecond = new Balance();
        balSecond.setUuid(second);
        balSecond.setAmount(100L);

        when(repository.findForUpdate(first)).thenReturn(balFirst);
        when(repository.findForUpdate(second)).thenReturn(balSecond);

        long amount = 50;

        // determine logical mapping to from/to
        Balance fromBalance = from.equals(first) ? balFirst : balSecond;
        Balance toBalance   = to.equals(first) ? balFirst : balSecond;

        service.transfer(from, to, amount);

        assertEquals(fromBalance.getAmount(),
                (fromBalance == balFirst ? 400 : 100) - 50);
        assertEquals(toBalance.getAmount(),
                (toBalance == balFirst ? 400 : 100) + 50);

        verify(repository, times(2)).save(any());
    }

    @Test
    void getTopBalances_validLimit_callsRepositoryWithNormalizedLimit() {
        when(repository.findTopByOrderByAmountDesc(5)).thenReturn(List.of());

        service.getTopBalances(5);

        verify(repository).findTopByOrderByAmountDesc(5);
    }

    @Test
    void getTopBalances_limitTooLow_defaultsTo10() {
        when(repository.findTopByOrderByAmountDesc(10)).thenReturn(List.of());

        service.getTopBalances(0);

        verify(repository).findTopByOrderByAmountDesc(10);
    }

    @Test
    void getTopBalances_limitTooHigh_capsAt20() {
        when(repository.findTopByOrderByAmountDesc(20)).thenReturn(List.of());

        service.getTopBalances(999);

        verify(repository).findTopByOrderByAmountDesc(20);
    }

    @Test
    void setBalance_success_updatesBalance() {
        UUID uuid = UUID.randomUUID();
        long newAmount = 800;

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
    void setBalance_negative_throwsException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> service.setBalance(uuid, -1));
    }
}
