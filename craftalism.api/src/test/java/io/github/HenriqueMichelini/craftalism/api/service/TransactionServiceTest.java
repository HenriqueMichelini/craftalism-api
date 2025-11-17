package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Service Tests")
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionService service;

    private UUID fromUuid;
    private UUID toUuid;
    private Balance fromBalance;
    private Balance toBalance;
    private TransactionRequestDTO requestDTO;
    private Transaction transaction;
    private TransactionResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        fromUuid = UUID.randomUUID();
        toUuid = UUID.randomUUID();

        fromBalance = new Balance();
        fromBalance.setUuid(fromUuid);
        fromBalance.setAmount(1000L);

        toBalance = new Balance();
        toBalance.setUuid(toUuid);
        toBalance.setAmount(500L);

        requestDTO = new TransactionRequestDTO(fromUuid, toUuid, 100L);

        transaction = new Transaction(fromUuid, toUuid, 100L);

        responseDTO = new TransactionResponseDTO(1L, fromUuid, toUuid, 100L, Instant.now());
    }

    // ========== PROCESS TRANSACTION TESTS ==========

    @Test
    @DisplayName("Should process transaction successfully")
    void shouldProcessTransactionSuccessfully() {
        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
        when(balanceService.updateBalance(eq(fromUuid), eq(900L))).thenReturn(fromBalance);
        when(balanceService.updateBalance(eq(toUuid), eq(600L))).thenReturn(toBalance);
        when(repository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.toDto(transaction)).thenReturn(responseDTO);

        TransactionResponseDTO result = service.processTransaction(requestDTO);

        assertNotNull(result);
        assertEquals(fromUuid, result.fromUuid());
        assertEquals(toUuid, result.toUuid());
        assertEquals(100L, result.amount());

        // Verify balance updates were called
        verify(balanceService, times(1)).getBalance(fromUuid);
        verify(balanceService, times(1)).getBalance(toUuid);
        verify(balanceService, times(1)).updateBalance(fromUuid, 900L);  // 1000 - 100
        verify(balanceService, times(1)).updateBalance(toUuid, 600L);    // 500 + 100
        verify(repository, times(1)).save(any(Transaction.class));
        verify(mapper, times(1)).toDto(transaction);
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void shouldThrowExceptionWhenAmountIsZero() {
        TransactionRequestDTO zeroAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, 0L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.processTransaction(zeroAmountDTO)
        );

        assertEquals("Amount must be greater than 0.", exception.getMessage());
        verify(balanceService, never()).getBalance(any());
        verify(balanceService, never()).updateBalance(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        TransactionRequestDTO negativeAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, -100L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.processTransaction(negativeAmountDTO)
        );

        assertEquals("Amount must be greater than 0.", exception.getMessage());
        verify(balanceService, never()).getBalance(any());
        verify(balanceService, never()).updateBalance(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when insufficient balance")
    void shouldThrowExceptionWhenInsufficientBalance() {
        TransactionRequestDTO largeAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, 2000L);
        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.processTransaction(largeAmountDTO)
        );

        assertEquals("Insufficient balance.", exception.getMessage());
        verify(balanceService, times(1)).getBalance(fromUuid);
        verify(balanceService, times(1)).getBalance(toUuid);
        verify(balanceService, never()).updateBalance(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when from balance not found")
    void shouldThrowExceptionWhenFromBalanceNotFound() {
        when(balanceService.getBalance(fromUuid))
                .thenThrow(new IllegalArgumentException("Balance not found for UUID: " + fromUuid));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.processTransaction(requestDTO)
        );

        assertTrue(exception.getMessage().contains("Balance not found"));
        verify(balanceService, times(1)).getBalance(fromUuid);
        verify(balanceService, never()).getBalance(toUuid);
        verify(balanceService, never()).updateBalance(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when to balance not found")
    void shouldThrowExceptionWhenToBalanceNotFound() {
        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
        when(balanceService.getBalance(toUuid))
                .thenThrow(new IllegalArgumentException("Balance not found for UUID: " + toUuid));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.processTransaction(requestDTO)
        );

        assertTrue(exception.getMessage().contains("Balance not found"));
        verify(balanceService, times(1)).getBalance(fromUuid);
        verify(balanceService, times(1)).getBalance(toUuid);
        verify(balanceService, never()).updateBalance(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should process transaction with exact balance amount")
    void shouldProcessTransactionWithExactBalanceAmount() {
        TransactionRequestDTO exactAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, 1000L);
        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
        when(balanceService.updateBalance(eq(fromUuid), eq(0L))).thenReturn(fromBalance);
        when(balanceService.updateBalance(eq(toUuid), eq(1500L))).thenReturn(toBalance);
        when(repository.save(any(Transaction.class))).thenReturn(transaction);
        when(mapper.toDto(any(Transaction.class))).thenReturn(responseDTO);

        TransactionResponseDTO result = service.processTransaction(exactAmountDTO);

        assertNotNull(result);
        verify(balanceService, times(1)).updateBalance(fromUuid, 0L);    // 1000 - 1000
        verify(balanceService, times(1)).updateBalance(toUuid, 1500L);   // 500 + 1000
        verify(repository, times(1)).save(any(Transaction.class));
    }

    // ========== GET ALL TRANSACTIONS TESTS ==========

    @Test
    @DisplayName("Should return all transactions")
    void shouldReturnAllTransactions() {
        Transaction tx1 = new Transaction(fromUuid, toUuid, 100L);
        Transaction tx2 = new Transaction(toUuid, fromUuid, 50L);
        List<Transaction> transactions = Arrays.asList(tx1, tx2);

        when(repository.findAll()).thenReturn(transactions);

        List<Transaction> result = service.getAllTransactions();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no transactions exist")
    void shouldReturnEmptyListWhenNoTransactionsExist() {
        when(repository.findAll()).thenReturn(List.of());

        List<Transaction> result = service.getAllTransactions();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAll();
    }

    // ========== GET TRANSACTION BY ID TESTS ==========

    @Test
    @DisplayName("Should return transaction when id exists")
    void shouldReturnTransactionWhenIdExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(transaction));

        Transaction result = service.getTransactionById(1L);

        assertNotNull(result);
        assertEquals(fromUuid, result.getFromUuid());
        assertEquals(toUuid, result.getToUuid());
        assertEquals(100L, result.getAmount());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when transaction not found by id")
    void shouldThrowExceptionWhenTransactionNotFoundById() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getTransactionById(999L)
        );

        assertTrue(exception.getMessage().contains("Transaction not found for id"));
        assertTrue(exception.getMessage().contains("999"));
        verify(repository, times(1)).findById(999L);
    }

    // ========== GET TRANSACTIONS BY FROM UUID TESTS ==========

    @Test
    @DisplayName("Should return transactions by from UUID")
    void shouldReturnTransactionsByFromUuid() {
        Transaction tx1 = new Transaction(fromUuid, toUuid, 100L);
        Transaction tx2 = new Transaction(fromUuid, toUuid, 200L);
        List<Transaction> transactions = Arrays.asList(tx1, tx2);

        when(repository.findByFromUuid(fromUuid)).thenReturn(transactions);

        List<Transaction> result = service.getTransactionsByFromUuid(fromUuid);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findByFromUuid(fromUuid);
    }

    @Test
    @DisplayName("Should return empty list when no transactions from UUID")
    void shouldReturnEmptyListWhenNoTransactionsFromUuid() {
        when(repository.findByFromUuid(fromUuid)).thenReturn(List.of());

        List<Transaction> result = service.getTransactionsByFromUuid(fromUuid);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByFromUuid(fromUuid);
    }

    // ========== GET TRANSACTIONS BY TO UUID TESTS ==========

    @Test
    @DisplayName("Should return transactions by to UUID")
    void shouldReturnTransactionsByToUuid() {
        Transaction tx1 = new Transaction(fromUuid, toUuid, 100L);
        Transaction tx2 = new Transaction(fromUuid, toUuid, 200L);
        List<Transaction> transactions = Arrays.asList(tx1, tx2);

        when(repository.findByToUuid(toUuid)).thenReturn(transactions);

        List<Transaction> result = service.getTransactionsByToUuid(toUuid);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findByToUuid(toUuid);
    }

    @Test
    @DisplayName("Should return empty list when no transactions to UUID")
    void shouldReturnEmptyListWhenNoTransactionsToUuid() {
        when(repository.findByToUuid(toUuid)).thenReturn(List.of());

        List<Transaction> result = service.getTransactionsByToUuid(toUuid);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findByToUuid(toUuid);
    }
}