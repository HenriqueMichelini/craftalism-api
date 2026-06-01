package io.github.HenriqueMichelini.craftalism.api.market.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.model.MarketEventEndReason;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventRarity;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventStatus;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MarketEventLifecycleServiceTest {

    @Mock
    private MarketEventInstanceRepository eventRepository;

    @Test
    void startMarksEventActiveAndUsesRepositoryGuard() {
        MarketEventInstance event = event(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z")
        );
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        MarketEventLifecycleService service = new MarketEventLifecycleService(
            eventRepository
        );

        MarketEventInstance started = service.start(event);

        assertEquals(MarketEventStatus.ACTIVE, started.getStatus());
        assertEquals("GLOBAL", started.getActiveSlot());
        verify(eventRepository).saveAndFlush(event);
    }

    @Test
    void startRejectsSecondActiveEventWhenRepositoryGuardFails() {
        MarketEventInstance event = event(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T01:00:00Z")
        );
        when(eventRepository.saveAndFlush(event)).thenThrow(
            new DataIntegrityViolationException("duplicate active event")
        );
        MarketEventLifecycleService service = new MarketEventLifecycleService(
            eventRepository
        );

        assertThrows(IllegalStateException.class, () -> service.start(event));
    }

    @Test
    void effectiveActiveEventDelegatesToStatusAndWallClockQuery() {
        Instant now = Instant.parse("2026-01-01T00:30:00Z");
        MarketEventLifecycleService service = new MarketEventLifecycleService(
            eventRepository
        );

        service.effectiveActiveEvent(now);

        verify(eventRepository).findEffectiveActive(now);
    }

    @Test
    void expireElapsedActiveEventsDelegatesCleanupWithoutPricingDependency() {
        Instant now = Instant.parse("2026-01-01T01:00:00Z");
        when(eventRepository.expireElapsedActiveEvents(now)).thenReturn(2);
        MarketEventLifecycleService service = new MarketEventLifecycleService(
            eventRepository
        );

        assertEquals(2, service.expireElapsedActiveEvents(now));
    }

    @Test
    void endClearsActiveSlotThroughTerminalStatus() {
        Instant now = Instant.parse("2026-01-01T01:00:00Z");
        MarketEventInstance event = event(
            Instant.parse("2026-01-01T00:00:00Z"),
            now
        );
        event.setStatus(MarketEventStatus.ACTIVE);
        MarketEventLifecycleService service = new MarketEventLifecycleService(
            eventRepository
        );

        service.end(
            event,
            MarketEventStatus.SUPERSEDED,
            MarketEventEndReason.SUPERSEDED,
            now
        );

        assertEquals(MarketEventStatus.SUPERSEDED, event.getStatus());
        assertEquals(MarketEventEndReason.SUPERSEDED, event.getEndReason());
        assertEquals(null, event.getActiveSlot());
        verify(eventRepository).save(event);
    }

    private MarketEventInstance event(Instant startedAt, Instant endsAt) {
        MarketEventInstance event = new MarketEventInstance();
        event.setTemplateId("template");
        event.setSource(MarketEventSource.SYSTEM);
        event.setRarity(MarketEventRarity.MEDIUM);
        event.setScope(MarketEventScope.MARKET_WIDE);
        event.setEffectBasisPoints(10_000);
        event.setEffectVersion(1);
        event.setBlocking(false);
        event.setStartedAt(startedAt);
        event.setEndsAt(endsAt);
        event.setCreatedAt(startedAt);
        event.setUpdatedAt(startedAt);
        event.setAuditMetadata("{}");
        return event;
    }
}
