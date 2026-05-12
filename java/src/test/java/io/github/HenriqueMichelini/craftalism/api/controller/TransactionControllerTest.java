package io.github.HenriqueMichelini.craftalism.api.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.service.TransactionService;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Unit tests for TransactionController.
 * <p>
 * Notes for your project:
 * - These tests use Mockito's @ExtendWith(MockitoExtension.class).
 * - They stub ServletUriComponentsBuilder.fromCurrentRequest() using Mockito's MockedStatic
 *   so the controller's URI-building in createTransaction() works in a unit test.
 * - If your DTO classes are Java records or final classes, and you get errors when mocking
 *   them, add the `mockito-inline` artifact to your test dependencies so Mockito can mock
 *   final types and static methods.
 */
@ExtendWith(MockitoExtension.class)
public class TransactionControllerTest {

    @Mock
    private TransactionService service;

    @Mock
    private TransactionMapper mapper;

    @InjectMocks
    private TransactionController controller;

    @Test
    void getAllTransactions_returnsOkAndFilteredPage() {
        TransactionResponseDTO dto = mock(TransactionResponseDTO.class);
        PageRequest pageable = PageRequest.of(0, 20);
        Page<TransactionResponseDTO> page = new PageImpl<>(List.of(dto));
        Instant createdFrom = Instant.parse("2026-05-01T00:00:00Z");
        Instant createdTo = Instant.parse("2026-05-12T23:59:59Z");

        when(service.findTransactions(any(), eq(pageable))).thenReturn(page);

        ResponseEntity<Page<TransactionResponseDTO>> resp =
            controller.getAllTransactions(
                "550e8400",
                "contains",
                "550e8400-e29b-41d4-a716-446655440001",
                "exact",
                100L,
                500L,
                createdFrom,
                createdTo,
                pageable
            );

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(page, resp.getBody());
        verify(service).findTransactions(
            argThat(filter ->
                filter.fromPlayerUuid().equals("550e8400") &&
                filter.fromPlayerUuidMatch().equals("contains") &&
                filter.toPlayerUuid().equals(
                    "550e8400-e29b-41d4-a716-446655440001"
                ) &&
                filter.toPlayerUuidMatch().equals("exact") &&
                filter.minAmount().equals(100L) &&
                filter.maxAmount().equals(500L) &&
                filter.createdFrom().equals(createdFrom) &&
                filter.createdTo().equals(createdTo)
            ),
            eq(pageable)
        );
    }

    @Test
    void getTransactionById_returnsOkAndMappedDto() {
        long id = 123L;
        Transaction tx = mock(Transaction.class);
        TransactionResponseDTO dto = mock(TransactionResponseDTO.class);

        when(service.getTransactionById(id)).thenReturn(tx);
        when(mapper.toDto(tx)).thenReturn(dto);

        ResponseEntity<TransactionResponseDTO> resp =
            controller.getTransactionById(id);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertSame(dto, resp.getBody());

        verify(service, times(1)).getTransactionById(id);
        verify(mapper, times(1)).toDto(tx);
    }

    @Test
    void getTransactionsByFromUuid_returnsOkAndMappedList() {
        UUID from = UUID.randomUUID();
        Transaction tx = mock(Transaction.class);
        TransactionResponseDTO dto = mock(TransactionResponseDTO.class);

        when(service.getTransactionsByFromUuid(from)).thenReturn(List.of(tx));
        when(mapper.toDto(tx)).thenReturn(dto);

        ResponseEntity<List<TransactionResponseDTO>> resp =
            controller.getTransactionsByFromUuid(from);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertSame(dto, resp.getBody().get(0));

        verify(service, times(1)).getTransactionsByFromUuid(from);
        verify(mapper, times(1)).toDto(tx);
    }

    @Test
    void getTransactionsByToUuid_returnsOkAndMappedList() {
        UUID to = UUID.randomUUID();
        Transaction tx = mock(Transaction.class);
        TransactionResponseDTO dto = mock(TransactionResponseDTO.class);

        when(service.getTransactionsByToUuid(to)).thenReturn(List.of(tx));
        when(mapper.toDto(tx)).thenReturn(dto);

        ResponseEntity<List<TransactionResponseDTO>> resp =
            controller.getTransactionsByToUuid(to);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertSame(dto, resp.getBody().get(0));

        verify(service, times(1)).getTransactionsByToUuid(to);
        verify(mapper, times(1)).toDto(tx);
    }

    @Test
    void createTransaction_returnsCreatedWithLocationAndBody() {
        TransactionRequestDTO req = mock(TransactionRequestDTO.class);
        TransactionResponseDTO created = mock(TransactionResponseDTO.class);

        when(service.processTransaction(req)).thenReturn(created);
        when(created.id()).thenReturn(555L);

        try (
            MockedStatic<ServletUriComponentsBuilder> mockedStatic =
                Mockito.mockStatic(ServletUriComponentsBuilder.class)
        ) {
            ServletUriComponentsBuilder builder = Mockito.mock(
                ServletUriComponentsBuilder.class
            );

            // create an actual UriComponents to return from buildAndExpand(...)
            UriComponents uriComponents = UriComponentsBuilder.fromUriString(
                "http://localhost/api/transactions/555"
            ).build();

            // static method returns the mock builder
            mockedStatic
                .when(ServletUriComponentsBuilder::fromCurrentRequest)
                .thenReturn(builder);

            // stub fluent methods
            when(builder.path("/{id}")).thenReturn(builder);
            when(builder.buildAndExpand(555L)).thenReturn(uriComponents);

            ResponseEntity<TransactionResponseDTO> resp =
                controller.createTransaction(req);

            assertEquals(HttpStatus.CREATED, resp.getStatusCode());
            assertSame(created, resp.getBody());

            URI location = resp.getHeaders().getLocation();
            assertNotNull(location);
            assertEquals(
                "http://localhost/api/transactions/555",
                location.toString()
            );

            verify(service, times(1)).processTransaction(req);
        }
    }
}
