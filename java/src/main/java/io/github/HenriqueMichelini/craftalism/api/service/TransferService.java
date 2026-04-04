package io.github.HenriqueMichelini.craftalism.api.service;

import io.github.HenriqueMichelini.craftalism.api.dto.BalanceTransferResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.api.exceptions.IdempotencyConflictException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidAmountException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.InvalidTransferException;
import io.github.HenriqueMichelini.craftalism.api.exceptions.MissingIdempotencyKeyException;
import io.github.HenriqueMichelini.craftalism.api.mapper.TransactionMapper;
import io.github.HenriqueMichelini.craftalism.api.model.Balance;
import io.github.HenriqueMichelini.craftalism.api.model.Transaction;
import io.github.HenriqueMichelini.craftalism.api.model.TransferIdempotencyRecord;
import io.github.HenriqueMichelini.craftalism.api.repository.BalanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransactionRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.TransferIdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final BalanceRepository balanceRepository;
    private final TransactionRepository transactionRepository;
    private final TransferIdempotencyRepository idempotencyRepository;
    private final TransactionMapper transactionMapper;
    private final TransferIncidentService incidentService;

    public TransferService(
        BalanceRepository balanceRepository,
        TransactionRepository transactionRepository,
        TransferIdempotencyRepository idempotencyRepository,
        TransactionMapper transactionMapper,
        TransferIncidentService incidentService
    ) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transactionMapper = transactionMapper;
        this.incidentService = incidentService;
    }

    @Transactional
    public BalanceTransferResponseDTO transfer(
        UUID from,
        UUID to,
        long amount,
        String idempotencyKey
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new MissingIdempotencyKeyException();
        }
        if (from.equals(to)) {
            throw new InvalidTransferException();
        }
        if (amount <= 0) {
            throw new InvalidAmountException();
        }

        String normalizedKey = idempotencyKey.trim();
        String requestHash = requestHash(from, to, amount);

        TransferIdempotencyRecord record = idempotencyRepository
            .findForUpdateByIdempotencyKey(normalizedKey)
            .orElseGet(() -> {
                TransferIdempotencyRecord created =
                    new TransferIdempotencyRecord();
                created.setIdempotencyKey(normalizedKey);
                created.setRequestHash(requestHash);
                created.setStatus(TransferIdempotencyRecord.Status.PROCESSING);
                return idempotencyRepository.save(created);
            });

        if (!record.getRequestHash().equals(requestHash)) {
            incidentService.recordIncident(
                "IDEMPOTENCY_CONFLICT",
                from,
                to,
                normalizedKey,
                "Idempotency key was reused with a different transfer payload",
                "expectedHash=" + record.getRequestHash() + ", actualHash=" + requestHash
            );
            throw new IdempotencyConflictException(normalizedKey);
        }

        if (
            record.getStatus() == TransferIdempotencyRecord.Status.COMPLETED &&
            record.getTransactionId() != null
        ) {
            Transaction persisted = transactionRepository
                .findById(record.getTransactionId())
                .orElseThrow(() -> new IllegalStateException(
                    "Idempotency record references missing transaction " +
                    record.getTransactionId()
                ));
            return new BalanceTransferResponseDTO(
                transactionMapper.toDto(persisted),
                true
            );
        }

        try {
            Transaction transaction = executeAtomicTransfer(from, to, amount);
            record.setStatus(TransferIdempotencyRecord.Status.COMPLETED);
            record.setTransactionId(transaction.getId());
            record.setUpdatedAt(Instant.now());
            idempotencyRepository.save(record);

            TransactionResponseDTO response = transactionMapper.toDto(
                transaction
            );
            return new BalanceTransferResponseDTO(response, false);
        } catch (RuntimeException ex) {
            incidentService.recordIncident(
                "TRANSFER_CRITICAL_FAILURE",
                from,
                to,
                normalizedKey,
                ex.getMessage(),
                "exception=" + ex.getClass().getName()
            );
            throw ex;
        }
    }

    private Transaction executeAtomicTransfer(UUID from, UUID to, long amount) {
        UUID first = from.compareTo(to) < 0 ? from : to;
        UUID second = first.equals(from) ? to : from;

        Balance firstBalance = balanceRepository
            .findForUpdate(first)
            .orElseThrow(() ->
                new io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceNotFoundException(
                    first
                )
            );
        Balance secondBalance = balanceRepository
            .findForUpdate(second)
            .orElseThrow(() ->
                new io.github.HenriqueMichelini.craftalism.api.exceptions.BalanceNotFoundException(
                    second
                )
            );

        Balance fromBalance = from.equals(first) ? firstBalance : secondBalance;
        Balance toBalance = to.equals(first) ? firstBalance : secondBalance;

        if (fromBalance.getAmount() < amount) {
            throw new io.github.HenriqueMichelini.craftalism.api.exceptions.InsufficientFundsException(
                from,
                amount
            );
        }

        fromBalance.setAmount(fromBalance.getAmount() - amount);
        toBalance.setAmount(toBalance.getAmount() + amount);
        balanceRepository.save(fromBalance);
        balanceRepository.save(toBalance);

        Transaction transaction = new Transaction(from, to, amount);
        return transactionRepository.save(transaction);
    }

    private String requestHash(UUID from, UUID to, long amount) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload = from + "|" + to + "|" + amount;
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }
}
