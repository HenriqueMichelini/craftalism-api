//package io.github.HenriqueMichelini.craftalism.api.service;
//
//import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
//import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
//import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
//import io.github.HenriqueMichelini.craftalism.api.model.Balance;
//import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
//import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("Transaction Service Tests")
//class TransactionServiceTest {
//
//    @Mock
//    private TransactionRepository repository;
//
//    @Mock
//    private BalanceService balanceService;
//
//    @Mock
//    private TransactionMapper mapper;
//
//    @InjectMocks
//    private TransactionService service;
//
//    private UUID fromUuid;
//    private UUID toUuid;
//    private Balance fromBalance;
//    private Balance toBalance;
//    private TransactionRequestDTO requestDTO;
//    private Transaction transaction;
//    private TransactionResponseDTO responseDTO;
//
//    @BeforeEach
//    void setUp() {
//        fromUuid = UUID.randomUUID();
//        toUuid = UUID.randomUUID();
//
//        fromBalance = new Balance();
//        fromBalance.setUuid(fromUuid);
//        fromBalance.setAmount(1000L);
//
//        toBalance = new Balance();
//        toBalance.setUuid(toUuid);
//        toBalance.setAmount(500L);
//
//        requestDTO = new TransactionRequestDTO(fromUuid, toUuid, 100L);
//
//        transaction = new Transaction(fromBalance, toBalance, 100L);
//
//        responseDTO = new TransactionResponseDTO(1L, fromUuid, toUuid, 100L, java.time.Instant.now());
//    }
//
//    @Test
//    @DisplayName("Should process transaction successfully")
//    void shouldProcessTransactionSuccessfully() {
//        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
//        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
//        when(repository.save(any(Transaction.class))).thenReturn(transaction);
//        when(mapper.toDto(transaction)).thenReturn(responseDTO);
//
//        TransactionResponseDTO result = service.processTransaction(requestDTO);
//
//        assertNotNull(result);
//        assertEquals(fromUuid, result.fromUuid());
//        assertEquals(toUuid, result.toUuid());
//        assertEquals(100L, result.amount());
//
//        // Verify balances were updated correctly
//        assertEquals(900L, fromBalance.getAmount()); // 1000 - 100
//        assertEquals(600L, toBalance.getAmount());   // 500 + 100
//
//        verify(balanceService, times(1)).getBalance(fromUuid);
//        verify(balanceService, times(1)).getBalance(toUuid);
//        verify(repository, times(1)).save(any(Transaction.class));
//        verify(mapper, times(1)).toDto(transaction);
//    }
//
//    @Test
//    @DisplayName("Should throw exception when amount is zero")
//    void shouldThrowExceptionWhenAmountIsZero() {
//        TransactionRequestDTO zeroAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, 0L);
//        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
//        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> service.processTransaction(zeroAmountDTO)
//        );
//
//        assertEquals("Amount must be greater than 0.", exception.getMessage());
//        verify(repository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when amount is negative")
//    void shouldThrowExceptionWhenAmountIsNegative() {
//        TransactionRequestDTO negativeAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, -100L);
//        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
//        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> service.processTransaction(negativeAmountDTO)
//        );
//
//        assertEquals("Amount must be greater than 0.", exception.getMessage());
//        verify(repository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when insufficient balance")
//    void shouldThrowExceptionWhenInsufficientBalance() {
//        TransactionRequestDTO largeAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, 2000L);
//        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
//        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> service.processTransaction(largeAmountDTO)
//        );
//
//        assertEquals("Insufficient balance.", exception.getMessage());
//        assertEquals(1000L, fromBalance.getAmount()); // Balance should not change
//        assertEquals(500L, toBalance.getAmount());     // Balance should not change
//        verify(repository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when from balance not found")
//    void shouldThrowExceptionWhenFromBalanceNotFound() {
//        when(balanceService.getBalance(fromUuid))
//                .thenThrow(new IllegalArgumentException("Balance not found for UUID: " + fromUuid));
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> service.processTransaction(requestDTO)
//        );
//
//        assertTrue(exception.getMessage().contains("Balance not found"));
//        verify(balanceService, times(1)).getBalance(fromUuid);
//        verify(balanceService, never()).getBalance(toUuid);
//        verify(repository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should throw exception when to balance not found")
//    void shouldThrowExceptionWhenToBalanceNotFound() {
//        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
//        when(balanceService.getBalance(toUuid))
//                .thenThrow(new IllegalArgumentException("Balance not found for UUID: " + toUuid));
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> service.processTransaction(requestDTO)
//        );
//
//        assertTrue(exception.getMessage().contains("Balance not found"));
//        verify(balanceService, times(1)).getBalance(fromUuid);
//        verify(balanceService, times(1)).getBalance(toUuid);
//        verify(repository, never()).save(any());
//    }
//
//    @Test
//    @DisplayName("Should process transaction with exact balance amount")
//    void shouldProcessTransactionWithExactBalanceAmount() {
//        TransactionRequestDTO exactAmountDTO = new TransactionRequestDTO(fromUuid, toUuid, 1000L);
//        when(balanceService.getBalance(fromUuid)).thenReturn(fromBalance);
//        when(balanceService.getBalance(toUuid)).thenReturn(toBalance);
//        when(repository.save(any(Transaction.class))).thenReturn(transaction);
//        when(mapper.toDto(any(Transaction.class))).thenReturn(responseDTO);
//
//        TransactionResponseDTO result = service.processTransaction(exactAmountDTO);
//
//        assertNotNull(result);
//        assertEquals(0L, fromBalance.getAmount());    // 1000 - 1000
//        assertEquals(1500L, toBalance.getAmount());   // 500 + 1000
//        verify(repository, times(1)).save(any(Transaction.class));
//    }
//
//    @Test
//    @DisplayName("Should return all transactions")
//    void shouldReturnAllTransactions() {
//        Transaction tx1 = new Transaction(fromBalance, toBalance, 100L);
//        Transaction tx2 = new Transaction(toBalance, fromBalance, 50L);
//        List<Transaction> transactions = Arrays.asList(tx1, tx2);
//
//        when(repository.findAll()).thenReturn(transactions);
//
//        List<Transaction> result = service.getAllTransactions();
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(repository, times(1)).findAll();
//    }
//
//    @Test
//    @DisplayName("Should return empty list when no transactions exist")
//    void shouldReturnEmptyListWhenNoTransactionsExist() {
//        when(repository.findAll()).thenReturn(List.of());
//
//        List<Transaction> result = service.getAllTransactions();
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(repository, times(1)).findAll();
//    }
//
//    @Test
//    @DisplayName("Should return transaction when id exists")
//    void shouldReturnTransactionWhenIdExists() {
//        when(repository.findById(1L)).thenReturn(Optional.of(transaction));
//
//        Transaction result = service.getTransactionById(1L);
//
//        assertNotNull(result);
//        assertEquals(fromBalance, result.getFromBalance());
//        assertEquals(toBalance, result.getToBalance());
//        assertEquals(100L, result.getAmount());
//        verify(repository, times(1)).findById(1L);
//    }
//
//    @Test
//    @DisplayName("Should throw exception when transaction not found by id")
//    void shouldThrowExceptionWhenTransactionNotFoundById() {
//        when(repository.findById(999L)).thenReturn(Optional.empty());
//
//        IllegalArgumentException exception = assertThrows(
//                IllegalArgumentException.class,
//                () -> service.getTransactionById(999L)
//        );
//
//        assertTrue(exception.getMessage().contains("Transaction not found for id"));
//        assertTrue(exception.getMessage().contains("999"));
//        verify(repository, times(1)).findById(999L);
//    }
//
//    @Test
//    @DisplayName("Should return transactions by from balance")
//    void shouldReturnTransactionsByFromBalance() {
//        Transaction tx1 = new Transaction(fromBalance, toBalance, 100L);
//        Transaction tx2 = new Transaction(fromBalance, toBalance, 200L);
//        List<Transaction> transactions = Arrays.asList(tx1, tx2);
//
//        when(repository.findByFromBalance(fromBalance)).thenReturn(transactions);
//
//        List<Transaction> result = service.getTransactionByFromBalance(fromBalance);
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(repository, times(1)).findByFromBalance(fromBalance);
//    }
//
//    @Test
//    @DisplayName("Should return empty list when no transactions from balance")
//    void shouldReturnEmptyListWhenNoTransactionsFromBalance() {
//        when(repository.findByFromBalance(fromBalance)).thenReturn(List.of());
//
//        List<Transaction> result = service.getTransactionByFromBalance(fromBalance);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(repository, times(1)).findByFromBalance(fromBalance);
//    }
//
//    @Test
//    @DisplayName("Should return transactions by to balance")
//    void shouldReturnTransactionsByToBalance() {
//        Transaction tx1 = new Transaction(fromBalance, toBalance, 100L);
//        Transaction tx2 = new Transaction(fromBalance, toBalance, 200L);
//        List<Transaction> transactions = Arrays.asList(tx1, tx2);
//
//        when(repository.findByToBalance(toBalance)).thenReturn(transactions);
//
//        List<Transaction> result = service.getTransactionByToBalance(toBalance);
//
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        verify(repository, times(1)).findByToBalance(toBalance);
//    }
//
//    @Test
//    @DisplayName("Should return empty list when no transactions to balance")
//    void shouldReturnEmptyListWhenNoTransactionsToBalance() {
//        when(repository.findByToBalance(toBalance)).thenReturn(List.of());
//
//        List<Transaction> result = service.getTransactionByToBalance(toBalance);
//
//        assertNotNull(result);
//        assertTrue(result.isEmpty());
//        verify(repository, times(1)).findByToBalance(toBalance);
//    }
//}