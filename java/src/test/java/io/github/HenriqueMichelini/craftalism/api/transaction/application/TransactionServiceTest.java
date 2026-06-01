package io.github.HenriqueMichelini.craftalism.api.transaction.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionFilterDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.PlayerNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TableFilterValidationException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.TransactionNotFoundException;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.repository.PlayerRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionService service;

    private TransactionFilterDTO emptyFilter;

    @BeforeEach
    void setUp() {
        emptyFilter = new TransactionFilterDTO(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    @Test
    void processTransaction_success_savesAndReturnsMappedDto() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        long amount = 123L;

        TransactionRequestDTO req = new TransactionRequestDTO(from, to, amount);

        Transaction savedTx = mock(Transaction.class);
        TransactionResponseDTO responseDto = mock(TransactionResponseDTO.class);

        when(playerRepository.existsById(from)).thenReturn(true);
        when(playerRepository.existsById(to)).thenReturn(true);
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
    void processTransaction_missingSender_throwsPlayerNotFoundException() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        TransactionRequestDTO req = new TransactionRequestDTO(from, to, 123L);

        when(playerRepository.existsById(from)).thenReturn(false);

        assertThrows(PlayerNotFoundException.class, () ->
            service.processTransaction(req)
        );

        verify(playerRepository).existsById(from);
        verify(playerRepository, never()).existsById(to);
        verifyNoInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void processTransaction_missingReceiver_throwsPlayerNotFoundException() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        TransactionRequestDTO req = new TransactionRequestDTO(from, to, 123L);

        when(playerRepository.existsById(from)).thenReturn(true);
        when(playerRepository.existsById(to)).thenReturn(false);

        assertThrows(PlayerNotFoundException.class, () ->
            service.processTransaction(req)
        );

        verify(playerRepository).existsById(from);
        verify(playerRepository).existsById(to);
        verifyNoInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void processTransaction_amountZeroOrNegative_throwsAndDoesNotCallDependencies() {
        TransactionRequestDTO req = mock(TransactionRequestDTO.class);
        when(req.amount()).thenReturn(0L); // test zero; negative case is equivalent

        assertThrows(InvalidAmountException.class, () ->
            service.processTransaction(req)
        );

        verifyNoInteractions(playerRepository);
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
    void findTransactions_withoutSort_appliesDefaultSortAndMapsPage() {
        Transaction tx = mock(Transaction.class);
        TransactionResponseDTO dto = mock(TransactionResponseDTO.class);
        PageRequest pageable = PageRequest.of(0, 20);
        when(repository.findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(tx)));
        when(mapper.toDto(tx)).thenReturn(dto);

        Page<TransactionResponseDTO> result = service.findTransactions(
            emptyFilter,
            pageable
        );

        assertEquals(List.of(dto), result.getContent());
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
            Pageable.class
        );
        verify(repository).findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            pageableCaptor.capture()
        );
        assertEquals(
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")),
            pageableCaptor.getValue().getSort()
        );
    }

    @Test
    void findTransactions_withExplicitAllowedSort_preservesSort() {
        PageRequest pageable = PageRequest.of(
            1,
            10,
            Sort.by(Sort.Order.asc("amount"))
        );
        when(repository.findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            any(Pageable.class)
        )).thenReturn(Page.empty(pageable));

        service.findTransactions(emptyFilter, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(
            Pageable.class
        );
        verify(repository).findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            pageableCaptor.capture()
        );
        assertEquals(pageable.getSort(), pageableCaptor.getValue().getSort());
    }

    @Test
    void findTransactions_acceptsComposableFiltersBeforePaging() {
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        TransactionFilterDTO filter = new TransactionFilterDTO(
            from.toString().substring(0, 8),
            "contains",
            to.toString(),
            "exact",
            100L,
            500L,
            Instant.parse("2026-05-01T00:00:00Z"),
            Instant.parse("2026-05-12T23:59:59Z")
        );
        PageRequest pageable = PageRequest.of(0, 5);
        when(repository.findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            any(Pageable.class)
        )).thenReturn(Page.empty(pageable));

        service.findTransactions(filter, pageable);

        verify(repository).findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            any(Pageable.class)
        );
    }

    @Test
    void findTransactions_rejectsInvalidRangesAndSort() {
        assertThrows(TableFilterValidationException.class, () ->
            service.findTransactions(
                new TransactionFilterDTO(
                    null,
                    null,
                    null,
                    null,
                    500L,
                    100L,
                    null,
                    null
                ),
                PageRequest.of(0, 20)
            )
        );

        assertThrows(TableFilterValidationException.class, () ->
            service.findTransactions(
                emptyFilter,
                PageRequest.of(0, 20, Sort.by("unsupported"))
            )
        );

        verify(repository, never()).findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            any(Pageable.class)
        );
    }

    @Test
    void findTransactions_rejectsInvalidExactUuid() {
        TransactionFilterDTO filter = new TransactionFilterDTO(
            "not-a-uuid",
            "exact",
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertThrows(TableFilterValidationException.class, () ->
            service.findTransactions(filter, PageRequest.of(0, 20))
        );

        verify(repository, never()).findAll(
            ArgumentMatchers.<Specification<Transaction>>any(),
            any(Pageable.class)
        );
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
