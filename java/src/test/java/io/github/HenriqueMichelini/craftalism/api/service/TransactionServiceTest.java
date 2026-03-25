package io.github.HenriqueMichelini.craftalism.api.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TransactionNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionService service;

    @Test
    void processTransaction_success_savesAndReturnsMappedDto() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        long amount = 123L;

        TransactionRequestDTO req = new TransactionRequestDTO(from, to, amount);

        Transaction savedTx = mock(Transaction.class);
        TransactionResponseDTO responseDto = mock(TransactionResponseDTO.class);

        when(repository.save(any(Transaction.class))).thenReturn(savedTx);
        when(mapper.toDto(savedTx)).thenReturn(responseDto);

        TransactionResponseDTO result = service.processTransaction(req);

        assertSame(responseDto, result);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(
            Transaction.class
        );

        verify(repository).save(captor.capture());
        Transaction tx = captor.getValue();

        assertEquals(from, tx.getFromPlayerUuid());
        assertEquals(to, tx.getToPlayerUuid());
        assertEquals(amount, tx.getAmount());

        verify(mapper).toDto(savedTx);
    }

    @Test
    void processTransaction_amountZeroOrNegative_throwsAndDoesNotCallDependencies() {
        TransactionRequestDTO req = mock(TransactionRequestDTO.class);
        when(req.amount()).thenReturn(0L); // test zero; negative case is equivalent

        assertThrows(InvalidAmountException.class, () ->
            service.processTransaction(req)
        );

        verifyNoInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void getAllTransactions_returnsRepositoryList() {
        List<Transaction> txs = List.of(
            mock(Transaction.class),
            mock(Transaction.class)
        );
        when(repository.findAll()).thenReturn(txs);

        List<Transaction> result = service.getAllTransactions();

        assertSame(txs, result);
        verify(repository).findAll();
        verifyNoInteractions(mapper);
    }

    @Test
    void getTransactionById_found_returnsTransaction() {
        Transaction tx = mock(Transaction.class);
        when(repository.findById(1L)).thenReturn(Optional.of(tx));

        Transaction result = service.getTransactionById(1L);

        assertSame(tx, result);
        verify(repository).findById(1L);
    }

    @Test
    void getTransactionById_notFound_throwsTransactionNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () ->
            service.getTransactionById(999L)
        );

        verify(repository).findById(999L);
    }

    @Test
    void getTransactionsByFromUuid_returnsRepositoryList() {
        UUID from = UUID.randomUUID();
        List<Transaction> list = List.of(mock(Transaction.class));
        when(repository.findByFromPlayerUuid(from)).thenReturn(list);

        List<Transaction> result = service.getTransactionsByFromUuid(from);

        assertSame(list, result);
        verify(repository).findByFromPlayerUuid(from);
    }

    @Test
    void getTransactionsByToUuid_returnsRepositoryList() {
        UUID to = UUID.randomUUID();
        List<Transaction> list = List.of(mock(Transaction.class));
        when(repository.findByToPlayerUuid(to)).thenReturn(list);

        List<Transaction> result = service.getTransactionsByToUuid(to);

        assertSame(list, result);
        verify(repository).findByToPlayerUuid(to);
    }
}
