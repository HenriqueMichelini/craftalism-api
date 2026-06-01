package io.github.HenriqueMichelini.craftalism.api.market.domain.event;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventEndReason;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketEventLifecycleService {

    private final MarketEventInstanceRepository eventRepository;

    public MarketEventLifecycleService(
        MarketEventInstanceRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public MarketEventInstance start(MarketEventInstance event) {
        validateActiveWindow(event);
        event.setStatus(MarketEventStatus.ACTIVE);
        event.setEndReason(null);
        event.setUpdatedAt(event.getCreatedAt());
        try {
            return eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                "A named market event is already active.",
                ex
            );
        }
    }

    @Transactional(readOnly = true)
    public Optional<MarketEventInstance> effectiveActiveEvent(Instant now) {
        return eventRepository.findEffectiveActive(now);
    }

    @Transactional
    public int expireElapsedActiveEvents(Instant now) {
        return eventRepository.expireElapsedActiveEvents(now);
    }

    @Transactional
    public MarketEventInstance end(
        MarketEventInstance event,
        MarketEventStatus status,
        MarketEventEndReason reason,
        Instant now
    ) {
        event.setStatus(status);
        event.setEndReason(reason);
        event.setUpdatedAt(now);
        return eventRepository.save(event);
    }

    private void validateActiveWindow(MarketEventInstance event) {
        if (
            event.getStartedAt() == null ||
            event.getEndsAt() == null ||
            !event.getEndsAt().isAfter(event.getStartedAt())
        ) {
            throw new IllegalArgumentException(
                "Market event end time must be after start time."
            );
        }
    }
}
