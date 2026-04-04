package io.github.HenriqueMichelini.craftalism.api.repository;

import io.github.HenriqueMichelini.craftalism.api.model.TransferIdempotencyRecord;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferIdempotencyRepository
    extends JpaRepository<TransferIdempotencyRecord, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        "SELECT r FROM TransferIdempotencyRecord r WHERE r.idempotencyKey = :idempotencyKey"
    )
    Optional<TransferIdempotencyRecord> findForUpdateByIdempotencyKey(
        @Param("idempotencyKey") String idempotencyKey
    );
}
