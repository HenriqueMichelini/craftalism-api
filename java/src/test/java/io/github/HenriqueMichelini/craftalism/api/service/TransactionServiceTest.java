package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private BalanceService balanceService;

    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionService service;

    @Test
    void processTransaction_success_callsTransferAndSavesAndReturnsMappedDto() {
        // arrange
        TransactionRequestDTO req = mock(TransactionRequestDTO.class);
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        long amount = 123L;

        when(req.fromPlayerUuid()).thenReturn(from);
        when(req.toPlayerUuid()).thenReturn(to);
        when(req.amount()).thenReturn(amount);

        Transaction savedTx = mock(Transaction.class);
        TransactionResponseDTO responseDto = mock(TransactionResponseDTO.class);

        when(repository.save(any(Transaction.class))).thenReturn(savedTx);
        when(mapper.toDto(savedTx)).thenReturn(responseDto);

        // act
        TransactionResponseDTO result = service.processTransaction(req);

        // assert
        assertSame(responseDto, result, "service should return the DTO produced by mapper");

        // verify interactions
        verify(balanceService, times(1)).transfer(from, to, amount);
        verify(repository, times(1)).save(any(Transaction.class));
        verify(mapper, times(1)).toDto(savedTx);

        // optionally capture the transaction passed to save (basic check it is a Transaction instance)
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue(), "saved transaction should not be null");
    }

    @Test
    void processTransaction_amountZeroOrNegative_throwsAndDoesNotCallDependencies() {
        TransactionRequestDTO req = mock(TransactionRequestDTO.class);
        when(req.amount()).thenReturn(0L); // test zero; negative case is equivalent

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.processTransaction(req));
        assertTrue(ex.getMessage().contains("greater than 0"));

        verifyNoInteractions(balanceService);
        verifyNoInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void getAllTransactions_returnsRepositoryList() {
        List<Transaction> txs = List.of(mock(Transaction.class), mock(Transaction.class));
        when(repository.findAll()).thenReturn(txs);

        List<Transaction> result = service.getAllTransactions();

        assertSame(txs, result);
        verify(repository, times(1)).findAll();
        verifyNoInteractions(balanceService, mapper);
    }

    @Test
    void getTransactionById_found_returnsTransaction() {
        Transaction tx = mock(Transaction.class);
        when(repository.findById(1L)).thenReturn(Optional.of(tx));

        Transaction result = service.getTransactionById(1L);

        assertSame(tx, result);
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void getTransactionById_notFound_throwsIllegalArgumentException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getTransactionById(999L));
        assertTrue(ex.getMessage().contains("Transaction not found for id: 999"));

        verify(repository, times(1)).findById(999L);
    }

    @Test
    void getTransactionsByFromUuid_returnsRepositoryList() {
        UUID from = UUID.randomUUID();
        List<Transaction> list = List.of(mock(Transaction.class));
        when(repository.findByFromPlayerUuid(from)).thenReturn(list);

        List<Transaction> result = service.getTransactionsByFromUuid(from);

        assertSame(list, result);
        verify(repository, times(1)).findByFromPlayerUuid(from);
    }

    @Test
    void getTransactionsByToUuid_returnsRepositoryList() {
        UUID to = UUID.randomUUID();
        List<Transaction> list = List.of(mock(Transaction.class));
        when(repository.findByToPlayerUuid(to)).thenReturn(list);

        List<Transaction> result = service.getTransactionsByToUuid(to);

        assertSame(list, result);
        verify(repository, times(1)).findByToPlayerUuid(to);
    }
}
