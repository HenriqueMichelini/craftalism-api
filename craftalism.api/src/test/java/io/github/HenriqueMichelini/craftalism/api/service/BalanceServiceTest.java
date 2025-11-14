package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Balance Service Tests")
class BalanceServiceTest {

    @Mock
    private BalanceRepository repository;

    @InjectMocks
    private BalanceService service;

    private UUID testUuid;
    private Balance testBalance;

    @BeforeEach
    void setUp() {
        testUuid = UUID.randomUUID();
        testBalance = new Balance();
        testBalance.setUuid(testUuid);
        testBalance.setAmount(100L);
    }

    @Test
    @DisplayName("Should return balance when UUID exists")
    void shouldReturnBalanceWhenUuidExists() {
        when(repository.findById(testUuid)).thenReturn(Optional.of(testBalance));

        Balance result = service.getBalance(testUuid);

        assertNotNull(result);
        assertEquals(testUuid, result.getUuid());
        assertEquals(100L, result.getAmount());
        verify(repository, times(1)).findById(testUuid);
    }

    @Test
    @DisplayName("Should throw exception when balance not found")
    void shouldThrowExceptionWhenBalanceNotFound() {
        UUID nonExistentUuid = UUID.randomUUID();
        when(repository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getBalance(nonExistentUuid)
        );

        assertTrue(exception.getMessage().contains("Balance not found for UUID"));
        assertTrue(exception.getMessage().contains(nonExistentUuid.toString()));
        verify(repository, times(1)).findById(nonExistentUuid);
    }

    @Test
    @DisplayName("Should create balance with initial amount zero")
    void shouldCreateBalanceWithInitialAmountZero() {
        UUID newUuid = UUID.randomUUID();
        Balance savedBalance = new Balance();
        savedBalance.setUuid(newUuid);
        savedBalance.setAmount(0L);

        when(repository.existsById(newUuid)).thenReturn(false);
        when(repository.save(any(Balance.class))).thenReturn(savedBalance);

        Balance result = service.createBalance(newUuid);

        assertNotNull(result);
        assertEquals(newUuid, result.getUuid());
        assertEquals(0L, result.getAmount());
        verify(repository, times(1)).existsById(newUuid);
        verify(repository, times(1)).save(any(Balance.class));
    }

    @Test
    @DisplayName("Should throw exception when balance already exists")
    void shouldThrowExceptionWhenBalanceAlreadyExists() {
        when(repository.existsById(testUuid)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createBalance(testUuid)
        );

        assertTrue(exception.getMessage().contains("Balance already exists for UUID"));
        assertTrue(exception.getMessage().contains(testUuid.toString()));
        verify(repository, times(1)).existsById(testUuid);
        verify(repository, never()).save(any(Balance.class));
    }

    @Test
    @DisplayName("Should update balance with valid amount")
    void shouldUpdateBalanceWithValidAmount() {
        Long newAmount = 500L;
        when(repository.findById(testUuid)).thenReturn(Optional.of(testBalance));
        when(repository.save(any(Balance.class))).thenReturn(testBalance);

        service.updateBalance(testUuid, newAmount);

        assertEquals(newAmount, testBalance.getAmount());
        verify(repository, times(1)).findById(testUuid);
        verify(repository, times(1)).save(testBalance);
    }

    @Test
    @DisplayName("Should update balance to zero")
    void shouldUpdateBalanceToZero() {
        Long newAmount = 0L;
        when(repository.findById(testUuid)).thenReturn(Optional.of(testBalance));
        when(repository.save(any(Balance.class))).thenReturn(testBalance);

        service.updateBalance(testUuid, newAmount);

        assertEquals(0L, testBalance.getAmount());
        verify(repository, times(1)).findById(testUuid);
        verify(repository, times(1)).save(testBalance);
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        Long negativeAmount = -100L;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateBalance(testUuid, negativeAmount)
        );

        assertEquals("Amount must be non-negative.", exception.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent balance")
    void shouldThrowExceptionWhenUpdatingNonExistentBalance() {
        UUID nonExistentUuid = UUID.randomUUID();
        Long newAmount = 100L;
        when(repository.findById(nonExistentUuid)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateBalance(nonExistentUuid, newAmount)
        );

        assertTrue(exception.getMessage().contains("Balance not found for UUID"));
        verify(repository, times(1)).findById(nonExistentUuid);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle large amount values")
    void shouldHandleLargeAmountValues() {
        Long largeAmount = Long.MAX_VALUE;
        when(repository.findById(testUuid)).thenReturn(Optional.of(testBalance));
        when(repository.save(any(Balance.class))).thenReturn(testBalance);

        service.updateBalance(testUuid, largeAmount);

        assertEquals(largeAmount, testBalance.getAmount());
        verify(repository, times(1)).findById(testUuid);
        verify(repository, times(1)).save(testBalance);
    }
}