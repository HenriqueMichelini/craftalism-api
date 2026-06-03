package io.github.HenriqueMichelini.craftalism.api.market.infrastructure.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.api.market.domain.event.MarketEventLifecycleService;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventInstance;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventScope;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventSource;
import io.github.HenriqueMichelini.craftalism.api.model.MarketEventTemplate;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventInstanceRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventSchedulerLockRepository;
import io.github.HenriqueMichelini.craftalism.api.repository.MarketEventTemplateRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

class MarketEventSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-04-12T18:00:00Z");

    private final MarketEventTemplateRepository templateRepository = mock(
        MarketEventTemplateRepository.class
    );
    private final MarketEventInstanceRepository eventRepository = mock(
        MarketEventInstanceRepository.class
    );
    private final MarketEventSchedulerLockRepository lockRepository = mock(
        MarketEventSchedulerLockRepository.class
    );
    private final MarketEventLifecycleService lifecycleService = mock(
        MarketEventLifecycleService.class
    );
    private final Random random = mock(Random.class);

    @Test
    void rollWindow_startsWeightedEligibleTemplateWhenLeaseAvailable() {
        MarketEventTemplate template = automaticTemplate(
            "farming_bumper_crop",
            MarketEventScope.CATEGORY,
            false
        );
        when(lockRepository.acquireExpiredLease(anyString(), anyString(), any(), any()))
            .thenReturn(1);
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.empty()
        );
        when(random.nextLong(anyLong())).thenReturn(0L);
        when(templateRepository.findAll()).thenReturn(List.of(template));
        when(eventRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(lifecycleService.start(any())).thenAnswer(invocation -> {
            MarketEventInstance event = invocation.getArgument(0);
            event.setId(99L);
            return event;
        });

        MarketEventScheduler.SchedulerDecision decision = scheduler(
            true,
            true,
            2_500L
        ).rollWindow();

        assertTrue(decision.started());
        assertEquals(99L, decision.eventId());
        assertEquals("farming_bumper_crop", decision.templateId());
        verify(lifecycleService).start(any(MarketEventInstance.class));
    }

    @Test
    void rollWindow_skipsWhenMarketClosed() {
        MarketEventScheduler.SchedulerDecision decision = scheduler(true, false)
            .rollWindow();

        assertFalse(decision.started());
        assertEquals("market_closed", decision.reason());
        verify(lockRepository, never()).acquireExpiredLease(
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void rollWindow_skipsWhenStartRollChoosesNoEvent() {
        when(lockRepository.acquireExpiredLease(anyString(), anyString(), any(), any()))
            .thenReturn(1);
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.empty()
        );
        when(random.nextLong(anyLong())).thenReturn(9_999L);

        MarketEventScheduler.SchedulerDecision decision = scheduler(
            true,
            true,
            2_500L
        ).rollWindow();

        assertFalse(decision.started());
        assertEquals("no_event_roll", decision.reason());
        verify(lifecycleService, never()).start(any());
    }

    @Test
    void rollWindow_waitsForNextJitteredWindowAfterDecision() {
        when(lockRepository.acquireExpiredLease(anyString(), anyString(), any(), any()))
            .thenReturn(1);
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.empty()
        );
        when(random.nextLong(10_000L)).thenReturn(9_999L);
        when(random.nextLong(1_801L)).thenReturn(900L);
        MarketEventScheduler scheduler = scheduler(true, true, 2_500L);

        MarketEventScheduler.SchedulerDecision firstDecision =
            scheduler.rollWindow();
        MarketEventScheduler.SchedulerDecision secondDecision =
            scheduler.rollWindow();

        assertEquals("no_event_roll", firstDecision.reason());
        assertEquals("window_not_due", secondDecision.reason());
        verify(lockRepository, times(1)).acquireExpiredLease(
            anyString(),
            anyString(),
            any(),
            any()
        );
    }

    @Test
    void rollWindow_duplicateExecutionsOnlyAcquireOneLease() {
        MarketEventTemplate template = automaticTemplate(
            "farming_bumper_crop",
            MarketEventScope.CATEGORY,
            false
        );
        when(lockRepository.acquireExpiredLease(anyString(), anyString(), any(), any()))
            .thenReturn(1, 0);
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.empty()
        );
        when(random.nextLong(anyLong())).thenReturn(0L);
        when(templateRepository.findAll()).thenReturn(List.of(template));
        when(eventRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(lifecycleService.start(any())).thenAnswer(invocation -> {
            MarketEventInstance event = invocation.getArgument(0);
            event.setId(99L);
            return event;
        });

        MarketEventScheduler first = scheduler(true, true);
        MarketEventScheduler second = scheduler(true, true);

        assertTrue(first.rollWindow().started());
        MarketEventScheduler.SchedulerDecision secondDecision =
            second.rollWindow();

        assertFalse(secondDecision.started());
        assertEquals("lease_unavailable", secondDecision.reason());
    }

    @Test
    void rollWindow_filtersBlockingTemplatesAndAutomaticDisabledTemplates() {
        MarketEventTemplate blockingTemplate = automaticTemplate(
            "customs_hold",
            MarketEventScope.ITEM,
            true
        );
        MarketEventTemplate disabledTemplate = automaticTemplate(
            "market_alarm",
            MarketEventScope.MARKET_WIDE,
            false
        );
        disabledTemplate.setAutomaticEnabled(false);
        when(lockRepository.acquireExpiredLease(anyString(), anyString(), any(), any()))
            .thenReturn(1);
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.empty()
        );
        when(random.nextLong(10_000L)).thenReturn(0L);
        when(templateRepository.findAll()).thenReturn(
            List.of(blockingTemplate, disabledTemplate)
        );

        MarketEventScheduler.SchedulerDecision decision = scheduler(true, true)
            .rollWindow();

        assertFalse(decision.started());
        assertEquals("no_eligible_templates", decision.reason());
    }

    @Test
    void rollWindow_loadsBoundedCooldownHistoryOnceForMultipleCandidates() {
        MarketEventTemplate shorterCooldown = automaticTemplate(
            "farming_bumper_crop",
            MarketEventScope.CATEGORY,
            false
        );
        shorterCooldown.setCooldownSeconds(3_600L);
        MarketEventTemplate longerCooldown = automaticTemplate(
            "farming_supply_shortage",
            MarketEventScope.CATEGORY,
            false
        );
        longerCooldown.setCooldownSeconds(7_200L);
        when(lockRepository.acquireExpiredLease(anyString(), anyString(), any(), any()))
            .thenReturn(1);
        when(lifecycleService.effectiveActiveEvent(any())).thenReturn(
            Optional.empty()
        );
        when(random.nextLong(anyLong())).thenReturn(0L);
        when(templateRepository.findAll()).thenReturn(
            List.of(shorterCooldown, longerCooldown)
        );
        when(eventRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(lifecycleService.start(any())).thenAnswer(invocation -> {
            MarketEventInstance event = invocation.getArgument(0);
            event.setId(99L);
            return event;
        });

        assertTrue(scheduler(true, true).rollWindow().started());

        verify(eventRepository, times(1)).findByCreatedAtAfter(
            eq(NOW.minusSeconds(7_200L))
        );
        verify(eventRepository, never()).findAll();
    }

    private MarketEventScheduler scheduler(
        boolean schedulerEnabled,
        boolean marketEnabled
    ) {
        return scheduler(schedulerEnabled, marketEnabled, 10_000L);
    }

    private MarketEventScheduler scheduler(
        boolean schedulerEnabled,
        boolean marketEnabled,
        long startChanceBasisPoints
    ) {
        return new MarketEventScheduler(
            templateRepository,
            eventRepository,
            lockRepository,
            lifecycleService,
            Clock.fixed(NOW, ZoneOffset.UTC),
            random,
            schedulerEnabled,
            marketEnabled,
            startChanceBasisPoints,
            Duration.ofSeconds(60L),
            Duration.ofSeconds(7_200L),
            Duration.ofSeconds(1_800L),
            "test-owner"
        );
    }

    private MarketEventTemplate automaticTemplate(
        String templateId,
        MarketEventScope scope,
        boolean blockingAllowed
    ) {
        MarketEventTemplate template = new MarketEventTemplate();
        template.setTemplateId(templateId);
        template.setScope(scope);
        template.setAutomaticEnabled(true);
        template.setAutomaticWeight(10);
        template.setBlockingAllowed(blockingAllowed);
        template.setMinDurationSeconds(1_800L);
        template.setMaxDurationSeconds(1_800L);
        template.setMinEffectBasisPoints(9_500);
        template.setMaxEffectBasisPoints(9_500);
        template.setEffectDirection(blockingAllowed ? "BLOCK" : "DOWN");
        template.setCooldownSeconds(7_200L);
        template.setPlayerFacingName("Event");
        template.setPlayerFacingDescription("Event description.");
        template.setBroadScopeHint("Farming goods");
        template.setEligibleTargetMetadata(
            scope == MarketEventScope.CATEGORY
                ? "{\"categoryIds\":[\"farming\"]}"
                : "{\"itemIds\":[\"wheat\"]}"
        );
        template.setCreatedAt(NOW);
        template.setUpdatedAt(NOW);
        return template;
    }
}
